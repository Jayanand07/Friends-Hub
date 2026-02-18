package com.example.socialmedia.service;

import com.example.socialmedia.dto.FollowUserResponse;
import com.example.socialmedia.entity.*;
import com.example.socialmedia.repository.*;

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
    private final AdminActionLogRepository adminActionLogRepository;

    public AdminService(UserRepository userRepository, PostRepository postRepository,
            CommentRepository commentRepository, BlockRepository blockRepository,
            AdminActionLogRepository adminActionLogRepository) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.blockRepository = blockRepository;
        this.adminActionLogRepository = adminActionLogRepository;
    }

    public List<FollowUserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(u -> {
                    String name = u.getUserInfo() != null
                            ? (u.getUserInfo().getFirstName() + " " + u.getUserInfo().getLastName())
                            : u.getEmail();
                    String pic = u.getUserInfo() != null ? u.getUserInfo().getProfilePicUrl() : null;
                    return new FollowUserResponse(u.getId(), name, pic);
                }).collect(Collectors.toList());
    }

    @Transactional
    public String deleteUser(Long userId, String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Admin not found"));
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        logAction(admin, "DELETE_USER", userId, null, "Deleted user: " + target.getEmail());
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
        logAction(admin, "BLOCK_USER", userId, null, "Blocked user: " + target.getEmail());
        return "User blocked by admin";
    }

    @Transactional
    public String adminUnblockUser(Long userId, String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Admin not found"));
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        blockRepository.findByBlockerAndBlocked(admin, target).ifPresent(blockRepository::delete);
        logAction(admin, "UNBLOCK_USER", userId, null, "Unblocked user: " + target.getEmail());
        return "User unblocked by admin";
    }

    @Transactional
    public String deletePost(Long postId, String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Admin not found"));
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

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

    public List<AdminActionLog> getActionLogs() {
        return adminActionLogRepository.findAllByOrderByCreatedAtDesc();
    }

    private void logAction(User admin, String actionType, Long targetUserId, Long targetPostId, String description) {
        AdminActionLog log = new AdminActionLog(admin, actionType, description);
        log.setTargetUserId(targetUserId);
        log.setTargetPostId(targetPostId);
        adminActionLogRepository.save(log);
    }
}
