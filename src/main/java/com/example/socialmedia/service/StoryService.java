package com.example.socialmedia.service;

import com.example.socialmedia.dto.StoryResponse;
import com.example.socialmedia.dto.StoryUserResponse;
import com.example.socialmedia.dto.FollowUserResponse;
import com.example.socialmedia.entity.Story;
import com.example.socialmedia.entity.StoryView;
import com.example.socialmedia.entity.User;
import com.example.socialmedia.entity.UserInfo;
import com.example.socialmedia.repository.*;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StoryService {

    private final StoryRepository storyRepository;
    private final StoryViewRepository storyViewRepository;
    private final UserRepository userRepository;
    private final UserInfoRepository userInfoRepository;
    private final FollowRepository followRepository;
    private final BlockRepository blockRepository;
    private final SupabaseStorageService storageService;

    public StoryService(StoryRepository storyRepository,
            StoryViewRepository storyViewRepository,
            UserRepository userRepository,
            UserInfoRepository userInfoRepository,
            FollowRepository followRepository,
            BlockRepository blockRepository,
            SupabaseStorageService storageService) {
        this.storyRepository = storyRepository;
        this.storyViewRepository = storyViewRepository;
        this.userRepository = userRepository;
        this.userInfoRepository = userInfoRepository;
        this.followRepository = followRepository;
        this.blockRepository = blockRepository;
        this.storageService = storageService;
    }

    @Transactional
    public StoryResponse uploadStory(String email, String imageUrl) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Story story = new Story(user, imageUrl);
        storyRepository.save(story);

        return new StoryResponse(story.getId(), story.getImageUrl(),
                story.getCreatedAt(), story.getExpiresAt(), 0, false);
    }

    @Transactional(readOnly = true)
    public List<StoryUserResponse> getActiveStories(String currentEmail) {
        User currentUser = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<Story> activeStories = storyRepository
                .findAllByExpiresAtAfterOrderByCreatedAtDesc(LocalDateTime.now());

        // Group stories by user
        Map<Long, List<Story>> storiesByUser = activeStories.stream()
                .collect(Collectors.groupingBy(s -> s.getUser().getId(),
                        LinkedHashMap::new, Collectors.toList()));

        List<StoryUserResponse> result = new ArrayList<>();

        // Put current user's stories first
        if (storiesByUser.containsKey(currentUser.getId())) {
            result.add(buildUserStories(currentUser, storiesByUser.get(currentUser.getId()), currentUser));
            storiesByUser.remove(currentUser.getId());
        }

        for (Map.Entry<Long, List<Story>> entry : storiesByUser.entrySet()) {
            User storyOwner = entry.getValue().get(0).getUser();

            // Block check: skip if either user blocked the other
            if (blockRepository.existsByBlockerAndBlocked(currentUser, storyOwner) ||
                    blockRepository.existsByBlockerAndBlocked(storyOwner, currentUser)) {
                continue;
            }

            // Private account check: skip if owner is private and viewer doesn't follow
            if (storyOwner.isPrivateAccount() &&
                    !followRepository.existsByFollowerAndFollowing(currentUser, storyOwner)) {
                continue;
            }

            // Story visibility: if owner restricts to followers only, check follow
            if (storyOwner.isAllowStoryViewByFollowersOnly() &&
                    !followRepository.existsByFollowerAndFollowing(currentUser, storyOwner)) {
                continue;
            }

            result.add(buildUserStories(storyOwner, entry.getValue(), currentUser));
        }

        return result;
    }

    @Transactional
    public void viewStory(Long storyId, String email) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found"));

        User viewer = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Don't track self-views
        if (story.getUser().getId().equals(viewer.getId())) {
            return;
        }

        // Block check
        if (blockRepository.existsByBlockerAndBlocked(story.getUser(), viewer) ||
                blockRepository.existsByBlockerAndBlocked(viewer, story.getUser())) {
            return;
        }

        if (!storyViewRepository.existsByStoryAndViewer(story, viewer)) {
            storyViewRepository.save(new StoryView(story, viewer));
        }
    }

    @Transactional(readOnly = true)
    public List<FollowUserResponse> getStoryViewers(Long storyId, String requesterEmail) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found"));
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // SECURITY: Only story owner can see viewers
        if (!story.getUser().getId().equals(requester.getId())) {
            throw new RuntimeException("Only the story owner can view story viewers");
        }

        List<StoryView> views = storyViewRepository.findByStoryId(storyId);

        // Batch load all UserInfo records for viewers in a single query (fixes N+1)
        List<Long> viewerIds = views.stream().map(v -> v.getViewer().getId()).collect(Collectors.toList());
        java.util.Map<Long, UserInfo> userInfoMap = new java.util.HashMap<>();
        if (!viewerIds.isEmpty()) {
            userInfoRepository.findByUserIdIn(viewerIds).forEach(ui -> userInfoMap.put(ui.getUser().getId(), ui));
        }

        return views.stream().map(v -> {
            User viewer = v.getViewer();
            UserInfo info = userInfoMap.get(viewer.getId());
            String name = info != null && info.getFirstName() != null
                    ? info.getFirstName() + (info.getLastName() != null ? " " + info.getLastName() : "")
                    : "User#" + viewer.getId();
            String pic = info != null ? info.getProfilePicUrl() : null;
            return new FollowUserResponse(viewer.getId(), name, pic);
        }).collect(Collectors.toList());
    }

    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredStories() {
        List<Story> expired = storyRepository.findAllByExpiresAtBefore(LocalDateTime.now());
        if (!expired.isEmpty()) {
            // Delete images from Supabase
            expired.forEach(s -> {
                if (s.getImageUrl() != null) {
                    storageService.deleteImage(s.getImageUrl());
                }
            });
            storyViewRepository.deleteByStoryIn(expired);
            storyRepository.deleteAll(expired);
        }
    }

    private StoryUserResponse buildUserStories(User user, List<Story> stories, User currentUser) {
        // Use lazy-loaded relationship instead of separate repository query
        UserInfo info = user.getUserInfo();
        String name = info != null && info.getFirstName() != null
                ? info.getFirstName() + (info.getLastName() != null ? " " + info.getLastName() : "")
                : "User#" + user.getId();
        String profilePic = info != null ? info.getProfilePicUrl() : null;

        // Batch query: get all view counts for these stories in 1 query (fixes N+1)
        List<Long> storyIds = stories.stream().map(Story::getId).collect(Collectors.toList());
        Map<Long, Long> viewCountMap = storyViewRepository.countViewsByStoryIds(storyIds).stream()
                .collect(Collectors.toMap(
                    arr -> ((Number) arr[0]).longValue(),
                    arr -> ((Number) arr[1]).longValue()));
        // Batch query: get all viewed-by-current-user statuses in 1 query (fixes N+1)
        Set<Long> viewedStoryIds = new java.util.HashSet<>(
                storyViewRepository.findViewedStoryIdsByStoryIdsAndViewer(storyIds, currentUser));

        List<StoryResponse> storyResponses = stories.stream().map(s -> {
            int viewerCount = viewCountMap.getOrDefault(s.getId(), 0L).intValue();
            boolean viewed = viewedStoryIds.contains(s.getId());
            return new StoryResponse(s.getId(), s.getImageUrl(),
                    s.getCreatedAt(), s.getExpiresAt(), viewerCount, viewed);
        }).collect(Collectors.toList());

        boolean hasUnviewed = storyResponses.stream().anyMatch(sr -> !sr.isViewedByCurrentUser());

        return new StoryUserResponse(user.getId(), name, profilePic, hasUnviewed, storyResponses);
    }

    private String getDisplayName(User user) {
        // Use lazy-loaded relationship instead of a separate repository query
        UserInfo info = user.getUserInfo();
        if (info != null && info.getFirstName() != null) {
            return info.getFirstName() + (info.getLastName() != null ? " " + info.getLastName() : "");
        }
        return user.getEmail().split("@")[0];
    }
}
