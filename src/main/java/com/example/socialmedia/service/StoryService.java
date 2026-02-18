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

    public List<FollowUserResponse> getStoryViewers(Long storyId) {
        List<StoryView> views = storyViewRepository.findByStoryId(storyId);
        return views.stream().map(v -> {
            User viewer = v.getViewer();
            String name = getDisplayName(viewer);
            UserInfo info = userInfoRepository.findByUser(viewer).orElse(null);
            String pic = info != null ? info.getProfilePicUrl() : null;
            return new FollowUserResponse(viewer.getId(), name, pic);
        }).collect(Collectors.toList());
    }

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
        String name = getDisplayName(user);
        UserInfo info = userInfoRepository.findByUser(user).orElse(null);
        String profilePic = info != null ? info.getProfilePicUrl() : null;

        List<StoryResponse> storyResponses = stories.stream().map(s -> {
            int viewerCount = storyViewRepository.findByStoryId(s.getId()).size();
            boolean viewed = storyViewRepository.existsByStoryAndViewer(s, currentUser);
            return new StoryResponse(s.getId(), s.getImageUrl(),
                    s.getCreatedAt(), s.getExpiresAt(), viewerCount, viewed);
        }).collect(Collectors.toList());

        boolean hasUnviewed = storyResponses.stream().anyMatch(sr -> !sr.isViewedByCurrentUser());

        return new StoryUserResponse(user.getId(), name, profilePic, hasUnviewed, storyResponses);
    }

    private String getDisplayName(User user) {
        UserInfo info = userInfoRepository.findByUser(user).orElse(null);
        if (info != null && info.getFirstName() != null) {
            return info.getFirstName() + (info.getLastName() != null ? " " + info.getLastName() : "");
        }
        return user.getEmail().split("@")[0];
    }
}
