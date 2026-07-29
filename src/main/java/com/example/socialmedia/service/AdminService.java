package com.example.socialmedia.service;

import com.example.socialmedia.dto.AdminActionLogResponse;
import com.example.socialmedia.dto.FollowUserResponse;
import com.example.socialmedia.entity.*;
import com.example.socialmedia.repository.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final BlockRepository blockRepository;
    private final FollowRepository followRepository;
    private final NotificationRepository notificationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AdminActionLogRepository adminActionLogRepository;
    private final SupabaseStorageService supabaseStorageService;

    public AdminService(UserRepository userRepository, PostRepository postRepository,
            CommentRepository commentRepository, BlockRepository blockRepository,
            FollowRepository followRepository, NotificationRepository notificationRepository,
            ChatMessageRepository chatMessageRepository, AdminActionLogRepository adminActionLogRepository,
            SupabaseStorageService supabaseStorageService) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.blockRepository = blockRepository;
        this.followRepository = followRepository;
        this.notificationRepository = notificationRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.adminActionLogRepository = adminActionLogRepository;
        this.supabaseStorageService = supabaseStorageService;
    }

    @Transactional(readOnly = true)
    public Page<FollowUserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(u -> {
                    String name = u.getUserInfo() != null
                            ? (u.getUserInfo().getFirstName() + " " + (u.getUserInfo().getLastName() != null ? u.getUserInfo().getLastName() : ""))
                            : u.getEmail();
                    String pic = u.getUserInfo() != null ? u.getUserInfo().getProfilePicUrl() : null;
                    return new FollowUserResponse(u.getId(), name.trim(), pic);
                });
    }

    @Transactional
    public String deleteUser(Long userId, String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Admin not found"));
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        logAction(admin, "DELETE_USER", userId, null, "Deleted user ID: " + userId);

        // Cascading deletion of related records to prevent FK constraint crashes
        followRepository.deleteByFollowerOrFollowing(target, target);
        blockRepository.deleteByBlockerOrBlocked(target, target);
        notificationRepository.deleteByUserOrActor(target, target);
        chatMessageRepository.deleteBySenderOrReceiver(target, target);

        userRepository.delete(target);
        return "User deleted";
    }

    @Transactional
    public String adminBlockUser(Long userId, String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Admin not found"));
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!blockRepository.existsByBlockerAndBlocked(admin, target)) {
            blockRepository.save(new Block(admin, target));
        }
        logAction(admin, "BLOCK_USER", userId, null, "Blocked user ID: " + userId);
        return "User blocked by admin";
    }

    @Transactional
    public String adminUnblockUser(Long userId, String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Admin not found"));
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        blockRepository.findByBlockerAndBlocked(admin, target).ifPresent(blockRepository::delete);
        logAction(admin, "UNBLOCK_USER", userId, null, "Unblocked user ID: " + userId);
        return "User unblocked by admin";
    }

    @Transactional
    public String deletePost(Long postId, String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Admin not found"));
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        if (post.getImageUrl() != null && !post.getImageUrl().isBlank()) {
            try {
                supabaseStorageService.deleteImage(post.getImageUrl());
            } catch (Exception ignored) {}
        }

        logAction(admin, "DELETE_POST", post.getUser().getId(), postId, "Deleted post #" + postId);
        postRepository.delete(post);
        return "Post deleted by admin";
    }

    @Transactional
    public String deleteComment(Long commentId, String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Admin not found"));
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        logAction(admin, "DELETE_COMMENT", comment.getUser().getId(), null, "Deleted comment #" + commentId);
        commentRepository.delete(comment);
        return "Comment deleted by admin";
    }

    @Transactional(readOnly = true)
    public List<AdminActionLogResponse> getActionLogs() {
        return adminActionLogRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(log -> {
                    String adminName = log.getAdmin() != null && log.getAdmin().getUserInfo() != null
                            ? (log.getAdmin().getUserInfo().getFirstName() + " " +
                               (log.getAdmin().getUserInfo().getLastName() != null ? log.getAdmin().getUserInfo().getLastName() : ""))
                            : "Admin#" + (log.getAdmin() != null ? log.getAdmin().getId() : 0);
                    return new AdminActionLogResponse(
                            log.getId(),
                            log.getAdmin() != null ? log.getAdmin().getId() : null,
                            adminName.trim(),
                            log.getActionType(),
                            log.getTargetUserId(),
                            log.getTargetPostId(),
                            log.getDescription(),
                            log.getCreatedAt());
                }).collect(Collectors.toList());
    }

    private void logAction(User admin, String actionType, Long targetUserId, Long targetPostId, String description) {
        AdminActionLog log = new AdminActionLog(admin, actionType, description);
        log.setTargetUserId(targetUserId);
        log.setTargetPostId(targetPostId);
        adminActionLogRepository.save(log);
    }
}
