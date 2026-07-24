package com.example.socialmedia.service;

import com.example.socialmedia.dto.PostRequest;
import com.example.socialmedia.dto.PostResponse;
import com.example.socialmedia.entity.Post;
import com.example.socialmedia.entity.Role;
import com.example.socialmedia.entity.User;
import com.example.socialmedia.entity.Notification.NotificationType;
import com.example.socialmedia.repository.PostRepository;
import com.example.socialmedia.repository.UserRepository;
import com.example.socialmedia.repository.LikeRepository;
import com.example.socialmedia.repository.CommentRepository;
import com.example.socialmedia.repository.SavedPostRepository;
import com.example.socialmedia.entity.Like;
import com.example.socialmedia.entity.Comment;
import com.example.socialmedia.entity.SavedPost;
import com.example.socialmedia.dto.CommentRequest;
import com.example.socialmedia.dto.CommentResponse;
import com.example.socialmedia.service.kafka.EventProducerService;
import com.example.socialmedia.dto.kafka.NotificationEvent;

import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@CacheConfig(cacheNames = "posts")
public class PostService {
        private static final Logger log =
        LoggerFactory.getLogger(PostService.class);

        private final PostRepository postRepository;
        private final UserRepository userRepository;
        private final LikeRepository likeRepository;
        private final CommentRepository commentRepository;
        private final SavedPostRepository savedPostRepository;
        private final ExternalApiService externalApiService;
        private final NotificationService notificationService;
        private final com.example.socialmedia.repository.FollowRepository followRepository;
        private final com.example.socialmedia.repository.BlockRepository blockRepository;
        private final EventProducerService eventProducerService;

        public PostService(PostRepository postRepository, UserRepository userRepository,
                        LikeRepository likeRepository, CommentRepository commentRepository,
                        SavedPostRepository savedPostRepository,
                        ExternalApiService externalApiService, NotificationService notificationService,
                        com.example.socialmedia.repository.FollowRepository followRepository,
                        com.example.socialmedia.repository.BlockRepository blockRepository,
                        EventProducerService eventProducerService) {
                this.postRepository = postRepository;
                this.userRepository = userRepository;
                this.likeRepository = likeRepository;
                this.commentRepository = commentRepository;
                this.savedPostRepository = savedPostRepository;
                this.externalApiService = externalApiService;
                this.notificationService = notificationService;
                this.followRepository = followRepository;
                this.blockRepository = blockRepository;
                this.eventProducerService = eventProducerService;
        }

        @Transactional
        @Caching(evict = {
                @CacheEvict(value = "posts", allEntries = true),
                @CacheEvict(value = "feed", allEntries = true),
                @CacheEvict(value = "userProfiles", allEntries = true)
        })
        public PostResponse createPost(PostRequest request, String userEmail) {
                User user = userRepository.findByEmail(userEmail)
                                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

                Post post = Post.builder()
                                .content(request.getContent())
                                .imageUrl(request.getImageUrl())
                                .user(user)
                                .build();

                Post savedPost = postRepository.save(post);

                externalApiService.notifyPostCreated(savedPost);

                // Send Kafka event for async processing
                // SECURITY: Use "POST" type instead of "LIKE" to avoid creating a
                // bogus self-notification for the post author.
                sendNotificationEvent(post.getUser().getId(), "POST", "POST",
                        user.getId(), savedPost.getId(), "Post created");

                return mapToPostResponse(savedPost, userEmail);
        }

        @Transactional(readOnly = true)
        @Cacheable(value = "feed", key = "'user:' + #currentUserEmail")
        public Page<PostResponse> getHomeFeed(String currentUserEmail, Pageable pageable) {
                User user = userRepository.findByEmail(currentUserEmail)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                // Get followings
                List<Long> followings = new java.util.ArrayList<>(followRepository.findByFollowerId(user.getId())
                                .stream().map(f -> f.getFollowing().getId()).toList());

                // Include self in feed
                followings.add(user.getId());

                Page<Post> postsPage = postRepository.findByUserIdIn(followings, pageable);
                return mapToPostResponsePage(postsPage, currentUserEmail);
        }

        @Transactional(readOnly = true)
        public Page<PostResponse> getAllPosts(Pageable pageable, String currentUserEmail) {
                Page<Post> postsPage = postRepository.findAllPublicPosts(pageable);
                return mapToPostResponsePage(postsPage, currentUserEmail);
        }

        @Transactional(readOnly = true)
        public Page<PostResponse> getPostsByUser(Long userId, Pageable pageable,
                        String viewerEmail) {
                java.util.Objects.requireNonNull(userId, "User ID cannot be null");
                User targetUser = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                User viewer = userRepository.findByEmail(viewerEmail)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                // 1. Block Check
                if (blockRepository.existsByBlockerAndBlocked(targetUser, viewer) ||
                                blockRepository.existsByBlockerAndBlocked(viewer, targetUser)) {
                        throw new RuntimeException("Content invalid or unavailable");
                }

                // 2. Private Check
                if (targetUser.isPrivateAccount() && !targetUser.getId().equals(viewer.getId())) {
                        boolean isFollowing = followRepository.existsByFollowerAndFollowing(viewer, targetUser);
                        if (!isFollowing) {
                                throw new RuntimeException("This account is private");
                        }
                }

                Page<Post> postsPage = postRepository.findByUserId(userId, pageable);
                return mapToPostResponsePage(postsPage, viewerEmail);
        }

        @Transactional
        @Caching(evict = {
                @CacheEvict(value = "posts", allEntries = true)
        })
        public String toggleLike(Long postId, String email) {
                User user = userRepository.findByEmailIgnoreCase(email)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                Post post = postRepository.findById(postId)
                                .orElseThrow(() -> new RuntimeException("Post not found"));

                // SECURITY: Block check — prevent blocked users from interacting
                if (blockRepository.existsByBlockerAndBlocked(post.getUser(), user) ||
                        blockRepository.existsByBlockerAndBlocked(user, post.getUser())) {
                        throw new RuntimeException("Action not allowed");
                }

                java.util.Optional<Like> existingLike = likeRepository.findByUserAndPost(user, post);

                if (existingLike.isPresent()) {
                        likeRepository.delete(existingLike.get());
                        return "Post unliked";
                } else {
                        Like like = new Like();
                        like.setUser(user);
                        like.setPost(post);
                        likeRepository.save(like);

                        // Save in-app notification & push real-time STOMP
                        try {
                                notificationService.createNotification(
                                        post.getUser(),
                                        NotificationType.LIKE,
                                        getDisplayName(user) + " liked your post",
                                        user,
                                        postId
                                );
                        } catch (Exception e) {
                                log.warn("Failed to create like notification: {}", e.getMessage());
                        }

                        // Send async notification via Kafka (non-blocking)
                        sendNotificationEvent(
                                post.getUser().getId(),
                                "POST",
                                "LIKE",
                                user.getId(),
                                postId,
                                getDisplayName(user) + " liked your post"
                        );

                        return "Post liked";
                }
        }

        @Transactional
        @Caching(evict = {
                @CacheEvict(value = "posts", allEntries = true)
        })
        public String toggleSavePost(Long postId, String email) {
                User user = userRepository.findByEmailIgnoreCase(email)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                Post post = postRepository.findById(postId)
                                .orElseThrow(() -> new RuntimeException("Post not found"));

                // SECURITY: Block check
                if (blockRepository.existsByBlockerAndBlocked(post.getUser(), user) ||
                        blockRepository.existsByBlockerAndBlocked(user, post.getUser())) {
                        throw new RuntimeException("Action not allowed");
                }

                Optional<SavedPost> existingSaved = savedPostRepository.findByUserAndPost(user, post);

                if (existingSaved.isPresent()) {
                        savedPostRepository.delete(existingSaved.get());
                        return "Post unsaved";
                } else {
                        SavedPost savedPost = new SavedPost(user, post);
                        savedPostRepository.save(savedPost);
                        return "Post saved";
                }
        }

        @Transactional(readOnly = true)
        public Page<PostResponse> getSavedPosts(String email, Pageable pageable) {
                Page<Post> postsPage = savedPostRepository.findSavedPostsByUserEmail(email, pageable);
                return mapToPostResponsePage(postsPage, email);
        }

        @Transactional
        @Caching(evict = {
                @CacheEvict(value = "posts", allEntries = true)
        })
        public void addComment(Long postId, CommentRequest request, String email) {
                User user = userRepository.findByEmailIgnoreCase(email)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                Post post = postRepository.findById(postId)
                                .orElseThrow(() -> new RuntimeException("Post not found"));

                // SECURITY: Block check — prevent blocked users from commenting
                if (blockRepository.existsByBlockerAndBlocked(post.getUser(), user) ||
                        blockRepository.existsByBlockerAndBlocked(user, post.getUser())) {
                        throw new RuntimeException("Action not allowed");
                }

                Comment comment = new Comment();
                comment.setContent(request.getContent());
                comment.setUser(user);
                comment.setPost(post);

                commentRepository.save(comment);

                // Save in-app notification & push real-time STOMP
                try {
                        notificationService.createNotification(
                                post.getUser(),
                                NotificationType.COMMENT,
                                getDisplayName(user) + " commented on your post",
                                user,
                                postId
                        );
                } catch (Exception e) {
                        log.warn("Failed to create comment notification: {}", e.getMessage());
                }

                // Send async notification via Kafka (non-blocking)
                sendNotificationEvent(
                        post.getUser().getId(),
                        "POST",
                        "COMMENT",
                        user.getId(),
                        postId,
                        getDisplayName(user) + " commented on your post"
                );
        }

        @Transactional
        @Caching(evict = {
                @CacheEvict(value = "posts", allEntries = true),
                @CacheEvict(value = "feed", allEntries = true),
                @CacheEvict(value = "userProfiles", allEntries = true)
        })
        public void deletePost(Long postId, String email) {
                Post post = postRepository.findById(postId)
                                .orElseThrow(() -> new RuntimeException("Post not found"));

                if (!post.getUser().getEmail().equals(email) && !isAdmin(email)) {
                        throw new RuntimeException("Unauthorized");
                }

                postRepository.delete(post);
        }

        @Transactional(readOnly = true)
        public Page<CommentResponse> getCommentsByPost(Long postId, Pageable pageable) {
                return commentRepository.findByPostId(postId, pageable)
                                .map(comment -> {
                                        String name = getDisplayName(comment.getUser());
                                        return new CommentResponse(
                                                        comment.getId(),
                                                        comment.getContent(),
                                                        name,
                                                        comment.getUser().getId(),
                                                        comment.getCreatedAt());
                                });
        }

        @Transactional
        public void deleteComment(Long commentId, String email) {
                Comment comment = commentRepository.findById(commentId)
                                .orElseThrow(() -> new RuntimeException("Comment not found"));

                if (!comment.getUser().getEmail().equals(email)) {
                        throw new RuntimeException("You can only delete your own comments");
                }

                commentRepository.delete(comment);
        }

        private boolean isAdmin(String email) {
                return userRepository.findByEmail(email)
                                .map(user -> user.getRole() == Role.ROLE_ADMIN)
                                .orElse(false);
        }

        private String getDisplayName(User user) {
                if (user.getUserInfo() != null && user.getUserInfo().getFirstName() != null) {
                        return user.getUserInfo().getFirstName() + " " +
                                        (user.getUserInfo().getLastName() != null ? user.getUserInfo().getLastName()
                                                        : "");
                }
                return user.getEmail().split("@")[0];
        }

        private Page<PostResponse> mapToPostResponsePage(Page<Post> page, String currentUserEmail) {
                List<Post> posts = page.getContent();
                if (posts.isEmpty()) {
                        return page.map(p -> null);
                }

                List<Long> postIds = posts.stream().map(Post::getId).collect(Collectors.toList());

                // 1 Batch query for liked status
                Set<Long> likedPostIds = (currentUserEmail != null && !currentUserEmail.isEmpty())
                                ? likeRepository.findLikedPostIdsByUserEmailAndPostIdIn(currentUserEmail, postIds)
                                : Collections.emptySet();

                // 1 Batch query for saved status
                Set<Long> savedPostIds = (currentUserEmail != null && !currentUserEmail.isEmpty())
                                ? savedPostRepository.findSavedPostIdsByUserEmailAndPostIdIn(currentUserEmail, postIds)
                                : Collections.emptySet();

                // 1 Batch query for like counts
                Map<Long, Long> likeCounts = likeRepository.countLikesByPostIdIn(postIds).stream()
                                .collect(Collectors.toMap(
                                    arr -> ((Number) arr[0]).longValue(),
                                    arr -> ((Number) arr[1]).longValue()));

                // 1 Batch query for comment counts
                Map<Long, Long> commentCounts = commentRepository.countCommentsByPostIdIn(postIds).stream()
                                .collect(Collectors.toMap(
                                    arr -> ((Number) arr[0]).longValue(),
                                    arr -> ((Number) arr[1]).longValue()));

                Map<Long, PostResponse> mappedMap = new HashMap<>();
                for (Post post : posts) {
                        String authorName;
                        if (post.getUser() != null && post.getUser().getUserInfo() != null && post.getUser().getUserInfo().getFirstName() != null) {
                                String fn = post.getUser().getUserInfo().getFirstName();
                                String ln = post.getUser().getUserInfo().getLastName();
                                authorName = (fn + " " + (ln != null ? ln : "")).trim();
                        } else if (post.getUser() != null) {
                                authorName = getDisplayName(post.getUser());
                        } else {
                                authorName = "Unknown";
                        }

                        boolean isLiked = likedPostIds.contains(post.getId());
                        boolean isSaved = savedPostIds.contains(post.getId());
                        long likeCount = likeCounts.getOrDefault(post.getId(), 0L);
                        long commentCount = commentCounts.getOrDefault(post.getId(), 0L);

                        PostResponse resp = PostResponse.builder()
                                        .id(post.getId())
                                        .content(post.getContent())
                                        .imageUrl(post.getImageUrl())
                                        .authorName(authorName)
                                        .authorId(post.getUser() != null ? post.getUser().getId() : null)
                                        .likeCount((int) likeCount)
                                        .commentCount((int) commentCount)
                                        .createdAt(post.getCreatedAt())
                                        .isLiked(isLiked)
                                        .isSaved(isSaved)
                                        .build();

                        mappedMap.put(post.getId(), resp);
                }

                return page.map(p -> mappedMap.get(p.getId()));
        }

        private PostResponse mapToPostResponse(Post post, String currentUserEmail) {
                boolean isLiked = likeRepository.existsByUserEmailAndPostId(currentUserEmail, post.getId());
                boolean isSaved = savedPostRepository.existsByUserEmailAndPostId(currentUserEmail, post.getId());
                return PostResponse.builder()
                                .id(post.getId())
                                .content(post.getContent())
                                .imageUrl(post.getImageUrl())
                                .authorName(post.getUser() != null ? getDisplayName(post.getUser()) : "Unknown")
                                .authorId(post.getUser().getId())
                                .likeCount(post.getLikes() != null ? post.getLikes().size() : 0)
                                .commentCount(post.getComments() != null ? post.getComments().size() : 0)
                                .createdAt(post.getCreatedAt())
                                .isLiked(isLiked)
                                .isSaved(isSaved)
                                .build();
        }

        // Helper to send async notification via Kafka
        private void sendNotificationEvent(Long targetUserId, String resourceType,
                        String type, Long actorId, Long resourceId, String content) {
                try {
                    eventProducerService.sendNotification(new NotificationEvent(
                            type, actorId, targetUserId, resourceId, resourceType, content, Instant.now()
                    ));
                } catch (Exception e) {
                        // Log but don't fail - Kafka is optional for notifications
                        log.warn("Failed to send async notification: {}", e.getMessage());
                }
        }
}
