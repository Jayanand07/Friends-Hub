package com.example.socialmedia.service;

import com.example.socialmedia.dto.CompatibilityResponse;
import com.example.socialmedia.dto.CompatibilityResponse.CompatibilityBadge;
import com.example.socialmedia.entity.User;
import com.example.socialmedia.entity.UserInfo;
import com.example.socialmedia.entity.Block;
import com.example.socialmedia.repository.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CompatibilityService {

    private final UserRepository userRepository;
    private final UserInfoRepository userInfoRepository;
    private final FollowRepository followRepository;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final BlockRepository blockRepository;

    public CompatibilityService(UserRepository userRepository,
                                UserInfoRepository userInfoRepository,
                                FollowRepository followRepository,
                                LikeRepository likeRepository,
                                CommentRepository commentRepository,
                                ChatMessageRepository chatMessageRepository,
                                BlockRepository blockRepository) {
        this.userRepository = userRepository;
        this.userInfoRepository = userInfoRepository;
        this.followRepository = followRepository;
        this.likeRepository = likeRepository;
        this.commentRepository = commentRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.blockRepository = blockRepository;
    }

    @Transactional(readOnly = true)
    public CompatibilityResponse getCompatibility(Long targetUserId, String requesterEmail) {
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // SECURITY: Block check — prevent compatibility computation with blocked users
        if (blockRepository.existsByBlockerAndBlocked(target, requester) ||
                blockRepository.existsByBlockerAndBlocked(requester, target)) {
            throw new RuntimeException("Action not allowed");
        }

        UserInfo requesterInfo = userInfoRepository.findByUser(requester).orElse(null);
        UserInfo targetInfo = userInfoRepository.findByUser(target).orElse(null);

        // ─── Factor Scores (each 0-20, total 0-100) ───

        // 1. Mutual Friends Score (0-20)
        Set<Long> myFollowingIds = followRepository.findByFollowerId(requester.getId())
                .stream().map(f -> f.getFollowing().getId()).collect(Collectors.toSet());
        long mutualCount = followRepository.findByFollowingId(target.getId()).stream()
                .map(f -> f.getFollower())
                .filter(u -> myFollowingIds.contains(u.getId()))
                .count();
        int mutualFriendScore = (int) Math.min(mutualCount * 4, 20);

        // 2. Location Score (0-20)
        int locationScore = 0;
        if (requesterInfo != null && targetInfo != null
                && requesterInfo.getLocation() != null && targetInfo.getLocation() != null
                && !requesterInfo.getLocation().isBlank() && !targetInfo.getLocation().isBlank()) {
            if (requesterInfo.getLocation().trim().equalsIgnoreCase(targetInfo.getLocation().trim())) {
                locationScore = 20;
            }
        }

        // 3. Interest Score (0-20) — based on bio keyword overlap
        Set<String> requesterKeywords = extractKeywords(requesterInfo != null ? requesterInfo.getBio() : null);
        Set<String> targetKeywords = extractKeywords(targetInfo != null ? targetInfo.getBio() : null);
        Set<String> sharedInterests = new HashSet<>(requesterKeywords);
        sharedInterests.retainAll(targetKeywords);
        int interestScore = (int) Math.min(sharedInterests.size() * 5, 20);

        // 4. Interaction Score (0-20) — likes + comments + messages between users
        long likesGiven = likeRepository.countLikesByLikerOnOwnerPosts(requester, target);
        long likesReceived = likeRepository.countLikesByLikerOnOwnerPosts(target, requester);
        long commentsGiven = commentRepository.countCommentsByCommenterOnOwnerPosts(requester.getId(), target.getId());
        long commentsReceived = commentRepository.countCommentsByCommenterOnOwnerPosts(target.getId(), requester.getId());
        long messages = chatMessageRepository.countConversation(requester, target);
        long totalInteractions = likesGiven + likesReceived + commentsGiven + commentsReceived + messages;
        int interactionScore = (int) Math.min(totalInteractions * 2, 20);

        // 5. Profile Completeness Score (0-20)
        int profileScore = 0;
        if (targetInfo != null) {
            if (targetInfo.getProfilePicUrl() != null && !targetInfo.getProfilePicUrl().isBlank()) profileScore += 4;
            if (targetInfo.getBio() != null && !targetInfo.getBio().isBlank()) profileScore += 4;
            if (targetInfo.getLocation() != null && !targetInfo.getLocation().isBlank()) profileScore += 4;
            if (targetInfo.getWebsite() != null && !targetInfo.getWebsite().isBlank()) profileScore += 4;
            if (targetInfo.getFirstName() != null && !targetInfo.getFirstName().isBlank()
                    && targetInfo.getLastName() != null && !targetInfo.getLastName().isBlank()) profileScore += 4;
        }

        // ─── Overall Score ───
        int overallScore = mutualFriendScore + locationScore + interestScore + interactionScore + profileScore;

        // ─── Label & Emoji ───
        String label;
        String emoji;
        if (overallScore >= 80) {
            label = "Best Friend";
            emoji = "💎";
        } else if (overallScore >= 60) {
            label = "Great Match";
            emoji = "🌟";
        } else if (overallScore >= 40) {
            label = "Good Friend";
            emoji = "🤝";
        } else if (overallScore >= 20) {
            label = "Acquaintance";
            emoji = "👋";
        } else {
            label = "New Connection";
            emoji = "🌱";
        }

        // ─── Badges ───
        List<CompatibilityBadge> badges = buildBadges(mutualCount, locationScore, sharedInterests.size(),
                totalInteractions, profileScore);

        return new CompatibilityResponse(overallScore, label, emoji,
                mutualFriendScore, locationScore, interestScore,
                interactionScore, profileScore, 0,
                new ArrayList<>(sharedInterests), badges);
    }

    private List<CompatibilityBadge> buildBadges(long mutualCount, int locationScore,
                                                  int interestCount, long totalInteractions,
                                                  int profileScore) {
        List<CompatibilityBadge> badges = new ArrayList<>();

        badges.add(new CompatibilityBadge("mutual_friends", "Mutual Friends",
                "👥", "Connected through " + mutualCount + " mutual friend" + (mutualCount != 1 ? "s" : ""),
                mutualCount > 0));

        badges.add(new CompatibilityBadge("same_location", "Same Location",
                "📍", "Share the same city or area",
                locationScore > 0));

        badges.add(new CompatibilityBadge("shared_interests", "Shared Interests",
                "🎯", "Share " + interestCount + " interest" + (interestCount != 1 ? "s" : ""),
                interestCount > 0));

        badges.add(new CompatibilityBadge("active_interaction", "Active Interaction",
                "💬", "Have exchanged " + totalInteractions + " interaction" + (totalInteractions != 1 ? "s" : ""),
                totalInteractions > 0));

        badges.add(new CompatibilityBadge("complete_profile", "Complete Profile",
                "✅", "Has a well-filled profile",
                profileScore >= 12));

        return badges;
    }

    private Set<String> extractKeywords(String text) {
        if (text == null || text.isBlank()) return new HashSet<>();
        return Arrays.stream(text.toLowerCase().split("[^a-z0-9]+"))
                .filter(w -> w.length() > 3)
                .collect(Collectors.toSet());
    }
}