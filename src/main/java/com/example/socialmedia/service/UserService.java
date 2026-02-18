package com.example.socialmedia.service;

import com.example.socialmedia.dto.FollowUserResponse;
import com.example.socialmedia.dto.UserProfileRequest;
import com.example.socialmedia.dto.UserProfileResponse;
import com.example.socialmedia.entity.*;
import com.example.socialmedia.entity.Notification.NotificationType;
import com.example.socialmedia.repository.*;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserInfoRepository userInfoRepository;
    private final FollowRepository followRepository;
    private final FollowRequestRepository followRequestRepository;
    private final BlockRepository blockRepository;
    private final NotificationService notificationService;
    private final SupabaseStorageService storageService;

    public UserService(UserRepository userRepository, UserInfoRepository userInfoRepository,
            FollowRepository followRepository, FollowRequestRepository followRequestRepository,
            BlockRepository blockRepository, NotificationService notificationService,
            SupabaseStorageService storageService) {
        this.userRepository = userRepository;
        this.userInfoRepository = userInfoRepository;
        this.followRepository = followRepository;
        this.followRequestRepository = followRequestRepository;
        this.blockRepository = blockRepository;
        this.notificationService = notificationService;
        this.storageService = storageService;
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    // ─── Profile ──────────────────────────────────────────────

    @Cacheable(value = "profiles", key = "#email")
    public UserProfileResponse getProfile(String email) {
        User user = getUserByEmail(email);
        UserInfo info = userInfoRepository.findByUser(user).orElse(null);
        return buildProfileResponse(user, info, user); // can view own profile
    }

    public UserProfileResponse getOtherProfile(Long targetId, String requesterEmail) {
        User requester = getUserByEmail(requesterEmail);
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        UserInfo info = userInfoRepository.findByUser(target).orElse(null);
        return buildProfileResponse(target, info, requester);
    }

    @Transactional
    @CacheEvict(value = "profiles", key = "#email")
    public UserProfileResponse updateProfile(String email, UserProfileRequest request) {
        User user = getUserByEmail(email);
        UserInfo info = userInfoRepository.findByUser(user).orElse(null);

        if (info == null) {
            info = new UserInfo();
            info.setUser(user);
        }

        info.setFirstName(request.getFirstName());
        info.setLastName(request.getLastName());
        info.setBio(request.getBio());
        info.setProfilePicUrl(request.getProfilePicUrl());
        if (request.getWebsite() != null)
            info.setWebsite(request.getWebsite());
        if (request.getLocation() != null)
            info.setLocation(request.getLocation());
        if (request.getGender() != null)
            info.setGender(request.getGender());
        if (request.getDob() != null)
            info.setDob(request.getDob());
        if (request.getPhone() != null)
            info.setPhone(request.getPhone());
        if (request.getProfileVisibility() != null) {
            try {
                info.setProfileVisibility(ProfileVisibility.valueOf(request.getProfileVisibility()));
            } catch (IllegalArgumentException ignored) {
            }
        }

        userInfoRepository.save(info);
        return buildProfileResponse(user, info, user);
    }

    @Transactional
    @CacheEvict(value = "profiles", key = "#email")
    public UserProfileResponse updateProfilePic(String email, String profilePicUrl) {
        User user = getUserByEmail(email);
        UserInfo info = userInfoRepository.findByUser(user).orElse(null);
        if (info == null) {
            info = new UserInfo();
            info.setUser(user);
        }

        // Delete old image if it exists and is from Supabase
        if (info.getProfilePicUrl() != null) {
            storageService.deleteImage(info.getProfilePicUrl());
        }

        info.setProfilePicUrl(profilePicUrl);
        userInfoRepository.save(info);
        return buildProfileResponse(user, info, user);
    }

    @Transactional
    @CacheEvict(value = "profiles", key = "#email")
    public UserProfileResponse removeProfilePic(String email) {
        User user = getUserByEmail(email);
        UserInfo info = userInfoRepository.findByUser(user).orElse(null);
        if (info != null && info.getProfilePicUrl() != null) {
            storageService.deleteImage(info.getProfilePicUrl());
            info.setProfilePicUrl(null);
            userInfoRepository.save(info);
        }
        return buildProfileResponse(user, info, user);
    }

    @Transactional
    @CacheEvict(value = "profiles", key = "#email")
    public UserProfileResponse updateProfileSettings(String email, UserProfileRequest request) {
        return updateProfile(email, request);
    }

    private UserProfileResponse buildProfileResponse(User user, UserInfo info, User requester) {
        boolean isMe = user.getId().equals(requester.getId());
        boolean isFollowing = followRepository.findByFollowerAndFollowing(requester, user).isPresent();
        boolean isFollowRequested = followRequestRepository.findByRequesterAndTarget(requester, user).isPresent();
        boolean canViewPosts = isMe || !user.isPrivateAccount() || isFollowing;

        return UserProfileResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(info != null ? info.getFirstName() : null)
                .lastName(info != null ? info.getLastName() : null)
                .bio(info != null ? info.getBio() : null)
                .profilePicUrl(info != null ? info.getProfilePicUrl() : null)
                .followerCount(followRepository.findByFollowingId(user.getId()).size())
                .followingCount(followRepository.findByFollowerId(user.getId()).size())
                .isPrivateAccount(user.isPrivateAccount())
                .isFollowing(isFollowing)
                .isFollowRequested(isFollowRequested)
                .canViewPosts(canViewPosts)
                .role(user.getRole())
                .build();
    }

    // ─── Follow / Unfollow ────────────────────────────────────

    @Transactional
    public String toggleFollow(Long targetUserId, String email) {
        User follower = getUserByEmail(email);
        User following = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (follower.getId().equals(following.getId())) {
            throw new RuntimeException("You cannot follow yourself");
        }

        // Block check
        if (isBlocked(follower, following) || isBlocked(following, follower)) {
            throw new RuntimeException("Action not allowed");
        }

        Optional<Follow> existingFollow = followRepository.findByFollowerAndFollowing(follower, following);

        if (existingFollow.isPresent()) {
            followRepository.delete(existingFollow.get());
            return "Unfollowed successfully";
        }

        // Private account → send follow request
        if (following.isPrivateAccount()) {
            Optional<FollowRequest> existing = followRequestRepository.findByRequesterAndTarget(follower, following);
            if (existing.isPresent()) {
                if (existing.get().getStatus() == FollowRequestStatus.PENDING) {
                    return "Follow request already sent";
                }
                followRequestRepository.delete(existing.get());
            }
            FollowRequest fr = new FollowRequest(follower, following);
            followRequestRepository.save(fr);
            notificationService.createNotification(
                    following, NotificationType.FOLLOW_REQUEST,
                    getDisplayName(follower) + " requested to follow you", follower);
            return "Follow request sent";
        }

        Follow follow = new Follow();
        follow.setFollower(follower);
        follow.setFollowing(following);
        followRepository.save(follow);

        notificationService.createNotification(
                following, NotificationType.FOLLOW,
                getDisplayName(follower) + " started following you", follower);

        return "Followed successfully";
    }

    // ─── Follow Requests ──────────────────────────────────────

    public List<FollowUserResponse> getPendingFollowRequests(String email) {
        User user = getUserByEmail(email);
        return followRequestRepository.findByTargetIdAndStatus(user.getId(), FollowRequestStatus.PENDING)
                .stream()
                .map(fr -> mapToFollowUserResponse(fr.getRequester()))
                .collect(Collectors.toList());
    }

    @Transactional
    public String acceptFollowRequest(Long requestId, String email) {
        User user = getUserByEmail(email);
        FollowRequest fr = followRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Follow request not found"));
        if (!fr.getTarget().getId().equals(user.getId())) {
            throw new RuntimeException("Not authorized");
        }
        fr.setStatus(FollowRequestStatus.ACCEPTED);
        followRequestRepository.save(fr);

        Follow follow = new Follow();
        follow.setFollower(fr.getRequester());
        follow.setFollowing(user);
        followRepository.save(follow);

        notificationService.createNotification(
                fr.getRequester(), NotificationType.FOLLOW,
                getDisplayName(user) + " accepted your follow request", user);

        return "Follow request accepted";
    }

    @Transactional
    public String rejectFollowRequest(Long requestId, String email) {
        User user = getUserByEmail(email);
        FollowRequest fr = followRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Follow request not found"));
        if (!fr.getTarget().getId().equals(user.getId())) {
            throw new RuntimeException("Not authorized");
        }
        fr.setStatus(FollowRequestStatus.REJECTED);
        followRequestRepository.delete(fr); // Prepare clean deletion for re-request
        return "Follow request rejected";
    }

    @Transactional
    public String acceptFollowRequestFromUser(Long requesterId, String email) {
        User user = getUserByEmail(email);
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        FollowRequest fr = followRequestRepository.findByRequesterAndTarget(requester, user)
                .orElseThrow(() -> new RuntimeException("Follow request not found"));

        if (fr.getStatus() != FollowRequestStatus.PENDING) {
            return "Request already processed";
        }

        fr.setStatus(FollowRequestStatus.ACCEPTED);
        followRequestRepository.save(fr);

        Follow follow = new Follow();
        follow.setFollower(fr.getRequester());
        follow.setFollowing(user);
        followRepository.save(follow);

        notificationService.createNotification(
                fr.getRequester(), NotificationType.FOLLOW,
                getDisplayName(user) + " accepted your follow request", user);

        return "Follow request accepted";
    }

    @Transactional
    public String rejectFollowRequestFromUser(Long requesterId, String email) {
        User user = getUserByEmail(email);
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        FollowRequest fr = followRequestRepository.findByRequesterAndTarget(requester, user)
                .orElseThrow(() -> new RuntimeException("Follow request not found"));

        followRequestRepository.delete(fr);
        return "Follow request rejected";
    }

    // ─── Privacy Toggle ───────────────────────────────────────

    @Transactional
    public Map<String, Object> togglePrivateAccount(String email) {
        User user = getUserByEmail(email);
        user.setPrivateAccount(!user.isPrivateAccount());
        userRepository.save(user);
        return Map.of("isPrivateAccount", user.isPrivateAccount());
    }

    // ─── Block / Unblock ──────────────────────────────────────

    @Transactional
    public String blockUser(Long targetUserId, String email) {
        User blocker = getUserByEmail(email);
        User blocked = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (blocker.getId().equals(blocked.getId())) {
            throw new RuntimeException("You cannot block yourself");
        }

        if (blockRepository.existsByBlockerAndBlocked(blocker, blocked)) {
            return "User already blocked";
        }

        blockRepository.save(new Block(blocker, blocked));

        // Auto-unfollow both directions
        followRepository.findByFollowerAndFollowing(blocker, blocked).ifPresent(followRepository::delete);
        followRepository.findByFollowerAndFollowing(blocked, blocker).ifPresent(followRepository::delete);

        return "User blocked";
    }

    @Transactional
    public String unblockUser(Long targetUserId, String email) {
        User blocker = getUserByEmail(email);
        User blocked = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        blockRepository.findByBlockerAndBlocked(blocker, blocked)
                .ifPresent(blockRepository::delete);

        return "User unblocked";
    }

    public boolean isBlocked(User a, User b) {
        return blockRepository.existsByBlockerAndBlocked(a, b);
    }

    public List<FollowUserResponse> getBlockedUsers(String email) {
        User user = getUserByEmail(email);
        return blockRepository.findByBlockerId(user.getId()).stream()
                .map(b -> mapToFollowUserResponse(b.getBlocked()))
                .collect(Collectors.toList());
    }

    // ─── Followers / Following ────────────────────────────────

    public List<FollowUserResponse> getFollowers(Long userId) {
        return followRepository.findByFollowingId(userId).stream()
                .map(follow -> mapToFollowUserResponse(follow.getFollower()))
                .collect(Collectors.toList());
    }

    public List<FollowUserResponse> getFollowing(Long userId) {
        return followRepository.findByFollowerId(userId).stream()
                .map(follow -> mapToFollowUserResponse(follow.getFollowing()))
                .collect(Collectors.toList());
    }

    public boolean isFollowing(Long followerId, Long followingId) {
        User follower = userRepository.findById(followerId).orElse(null);
        User following = userRepository.findById(followingId).orElse(null);
        if (follower == null || following == null)
            return false;
        return followRepository.findByFollowerAndFollowing(follower, following).isPresent();
    }

    // ─── Helpers ──────────────────────────────────────────────

    private String getDisplayName(User user) {
        if (user.getUserInfo() != null && user.getUserInfo().getFirstName() != null) {
            return user.getUserInfo().getFirstName() + " " +
                    (user.getUserInfo().getLastName() != null ? user.getUserInfo().getLastName() : "");
        }
        return user.getEmail().split("@")[0];
    }

    private FollowUserResponse mapToFollowUserResponse(User user) {
        String name;
        String profilePicUrl = null;
        if (user.getUserInfo() != null) {
            name = user.getUserInfo().getFirstName() + " " + user.getUserInfo().getLastName();
            profilePicUrl = user.getUserInfo().getProfilePicUrl();
        } else {
            name = user.getEmail();
        }
        return new FollowUserResponse(user.getId(), name, profilePicUrl);
    }
}
