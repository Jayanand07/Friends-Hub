package com.example.socialmedia.service;

import com.example.socialmedia.entity.*;
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

    public ReactionService(ReactionRepository reactionRepository, UserRepository userRepository) {
        this.reactionRepository = reactionRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Map<String, Object> addReaction(String email, String targetType, Long targetId, String emoji,
            String gifUrl) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        ReactionTargetType type = ReactionTargetType.valueOf(targetType.toUpperCase());

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
