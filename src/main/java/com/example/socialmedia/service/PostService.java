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
import com.example.socialmedia.entity.Like;
import com.example.socialmedia.entity.Comment;
import com.example.socialmedia.dto.CommentRequest;
import com.example.socialmedia.dto.CommentResponse;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {

        private final PostRepository postRepository;
        private final UserRepository userRepository;
        private final LikeRepository likeRepository;
        private final CommentRepository commentRepository;
        private final ExternalApiService externalApiService;
        private final NotificationService notificationService;
        private final com.example.socialmedia.repository.FollowRepository followRepository;
        private final com.example.socialmedia.repository.BlockRepository blockRepository;

        public PostService(PostRepository postRepository, UserRepository userRepository,
                        LikeRepository likeRepository, CommentRepository commentRepository,
                        ExternalApiService externalApiService, NotificationService notificationService,
                        com.example.socialmedia.repository.FollowRepository followRepository,
                        com.example.socialmedia.repository.BlockRepository blockRepository) {
                this.postRepository = postRepository;
                this.userRepository = userRepository;
                this.likeRepository = likeRepository;
                this.commentRepository = commentRepository;
                this.externalApiService = externalApiService;
                this.notificationService = notificationService;
                this.followRepository = followRepository;
                this.blockRepository = blockRepository;
        }

        @Transactional
        @CacheEvict(value = "posts", allEntries = true)
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

                return mapToPostResponse(savedPost, userEmail);
        }

        @Transactional(readOnly = true)
        public Page<PostResponse> getAllPosts(Pageable pageable, String currentUserEmail) {
                return postRepository.findAllPublicPosts(pageable)
                                .map(post -> mapToPostResponse(post, currentUserEmail));
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

                return postRepository.findByUserId(userId, pageable)
                                .map(post -> mapToPostResponse(post, viewerEmail));
        }

        @Transactional
        public String toggleLike(Long postId, String email) {
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                Post post = postRepository.findById(postId)
                                .orElseThrow(() -> new RuntimeException("Post not found"));

                java.util.Optional<Like> existingLike = likeRepository.findByUserAndPost(user, post);

                if (existingLike.isPresent()) {
                        likeRepository.delete(existingLike.get());
                        return "Post unliked";
                } else {
                        Like like = new Like();
                        like.setUser(user);
                        like.setPost(post);
                        likeRepository.save(like);

                        // Notify post owner
                        notificationService.createNotification(
                                        post.getUser(), NotificationType.LIKE,
                                        getDisplayName(user) + " liked your post", user);

                        return "Post liked";
                }
        }

        @Transactional
        public void addComment(Long postId, CommentRequest request, String email) {
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                Post post = postRepository.findById(postId)
                                .orElseThrow(() -> new RuntimeException("Post not found"));

                Comment comment = new Comment();
                comment.setContent(request.getContent());
                comment.setUser(user);
                comment.setPost(post);

                commentRepository.save(comment);

                // Notify post owner
                notificationService.createNotification(
                                post.getUser(), NotificationType.COMMENT,
                                getDisplayName(user) + " commented on your post", user);
        }

        @Transactional
        @CacheEvict(value = "posts", allEntries = true)
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
                                        String name = "Unknown";
                                        if (comment.getUser().getUserInfo() != null) {
                                                name = comment.getUser().getUserInfo().getFirstName() + " "
                                                                + comment.getUser().getUserInfo().getLastName();
                                        } else {
                                                name = comment.getUser().getEmail();
                                        }
                                        return new CommentResponse(
                                                        comment.getId(),
                                                        comment.getContent(),
                                                        name,
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

        private PostResponse mapToPostResponse(Post post, String currentUserEmail) {
                boolean isLiked = likeRepository.existsByUserEmailAndPostId(currentUserEmail, post.getId());
                return PostResponse.builder()
                                .id(post.getId())
                                .content(post.getContent())
                                .imageUrl(post.getImageUrl())
                                .authorName(post.getUser().getUserInfo() != null
                                                ? post.getUser().getUserInfo().getFirstName() + " "
                                                                + post.getUser().getUserInfo().getLastName()
                                                : post.getUser().getEmail())
                                .authorId(post.getUser().getId())
                                .likeCount(post.getLikes() != null ? post.getLikes().size() : 0)
                                .commentCount(post.getComments() != null ? post.getComments().size() : 0)
                                .createdAt(post.getCreatedAt())
                                .isLiked(isLiked)
                                .build();
        }
}
