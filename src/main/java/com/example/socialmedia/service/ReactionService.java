package com.example.socialmedia.service;

import com.example.socialmedia.entity.*;
import com.example.socialmedia.repository.BlockRepository;
import com.example.socialmedia.repository.PostRepository;
import com.example.socialmedia.repository.ReactionRepository;
import com.example.socialmedia.repository.UserRepository;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReactionService {

    private final ReactionRepository reactionRepository;
    private final UserRepository userRepository;
    private final BlockRepository blockRepository;
    private final PostRepository postRepository;

    public ReactionService(ReactionRepository reactionRepository, UserRepository userRepository,
            BlockRepository blockRepository, PostRepository postRepository) {
        this.reactionRepository = reactionRepository;
        this.userRepository = userRepository;
        this.blockRepository = blockRepository;
        this.postRepository = postRepository;
    }

    @Transactional
    public Map<String, Object> addReaction(String email, String targetType, Long targetId, String emoji,
            String gifUrl) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        ReactionTargetType type = ReactionTargetType.valueOf(targetType.toUpperCase());

        // SECURITY: Block check — determine the target content owner and verify neither
        // user has blocked the other
        User targetUser = null;
        if (type == ReactionTargetType.POST) {
            Post post = postRepository.findById(targetId)
                    .orElseThrow(() -> new RuntimeException("Target post not found"));
            targetUser = post.getUser();
        } else if (type == ReactionTargetType.COMMENT) {
            // For future comment reaction support, look up the comment's author
            // For now, skip block check for non-post targets
        }

        if (targetUser != null) {
            if (blockRepository.existsByBlockerAndBlocked(targetUser, user) ||
                    blockRepository.existsByBlockerAndBlocked(user, targetUser)) {
                throw new RuntimeException("Action not allowed");
            }
        }

        // Remove existing reaction by same user on same target
        reactionRepository.findByUserAndTargetTypeAndTargetId(user, type, targetId)
                .ifPresent(reactionRepository::delete);

        Reaction reaction = new Reaction();
        reaction.setUser(user);
        reaction.setTargetType(type);
        reaction.setTargetId(targetId);
        reaction.setEmoji(emoji);
        reaction.setGifUrl(gifUrl);
        reactionRepository.save(reaction);

        return Map.of("id", reaction.getId(), "emoji", emoji != null ? emoji : "", "gifUrl",
                gifUrl != null ? gifUrl : "");
    }

    @Transactional
    public void removeReaction(String email, String targetType, Long targetId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        ReactionTargetType type = ReactionTargetType.valueOf(targetType.toUpperCase());
        reactionRepository.deleteByUserAndTargetTypeAndTargetId(user, type, targetId);
    }

    public List<Map<String, Object>> getReactions(String targetType, Long targetId) {
        ReactionTargetType type = ReactionTargetType.valueOf(targetType.toUpperCase());
        return reactionRepository.findByTargetTypeAndTargetId(type, targetId).stream()
                .map(r -> {
                    String name = r.getUser().getUserInfo() != null
                            ? r.getUser().getUserInfo().getFirstName()
                            : r.getUser().getEmail().split("@")[0];
                    return Map.<String, Object>of(
                            "id", r.getId(),
                            "userId", r.getUser().getId(),
                            "userName", name,
                            "emoji", r.getEmoji() != null ? r.getEmoji() : "",
                            "gifUrl", r.getGifUrl() != null ? r.getGifUrl() : "");
                }).collect(Collectors.toList());
    }
}
