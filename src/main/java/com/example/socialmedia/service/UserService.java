package com.example.socialmedia.service;

import com.example.socialmedia.dto.FollowToggleResponse;
import com.example.socialmedia.dto.FollowUserResponse;
import com.example.socialmedia.dto.NetworkGraphResponse;
import com.example.socialmedia.dto.PublicKeyResponse;
import com.example.socialmedia.dto.RecommendationResponse;
import com.example.socialmedia.dto.SearchUserResponse;
import com.example.socialmedia.dto.UserProfileRequest;
import com.example.socialmedia.dto.UserProfileResponse;
import com.example.socialmedia.entity.*;
import com.example.socialmedia.entity.Notification.NotificationType;
import com.example.socialmedia.repository.*;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

    // ─── Profile Fetching ─────────────────────────────────────

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String email) {
        User user = getUserByEmail(email);
        UserInfo info = userInfoRepository.findByUser(user).orElse(null);
        return buildProfileResponse(user, info, user);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getOtherProfile(Long targetId, String requesterEmail) {
        User requester = getUserByEmail(requesterEmail);
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // SECURITY: Block check — prevent blocked users from viewing profile info
        if (blockRepository.existsByBlockerAndBlocked(target, requester) ||
                blockRepository.existsByBlockerAndBlocked(requester, target)) {
            throw new RuntimeException("Profile not available");
        }

        UserInfo info = userInfoRepository.findByUser(target).orElse(null);
        return buildProfileResponse(target, info, requester);
    }

    @Transactional
    public UserProfileResponse updateProfile(String email, UserProfileRequest request) {
        User user = getUserByEmail(email);
        UserInfo info = userInfoRepository.findByUser(user).orElseGet(() -> {
            UserInfo newInfo = new UserInfo();
            newInfo.setUser(user);
            return newInfo;
        });

        if (request.getFirstName() != null) info.setFirstName(request.getFirstName());
        if (request.getLastName() != null) info.setLastName(request.getLastName());
        if (request.getBio() != null) info.setBio(request.getBio());

        userInfoRepository.save(info);
        return buildProfileResponse(user, info, user);
    }

    @Transactional
    public UserProfileResponse updateProfilePic(String email, String profilePicUrl) {
        User user = getUserByEmail(email);
        UserInfo info = userInfoRepository.findByUser(user).orElseGet(() -> {
            UserInfo newInfo = new UserInfo();
            newInfo.setUser(user);
            return newInfo;
        });
        if (info.getProfilePicUrl() != null && storageService != null) {
            storageService.deleteImage(info.getProfilePicUrl());
        }
        info.setProfilePicUrl(profilePicUrl);
        userInfoRepository.save(info);
        return buildProfileResponse(user, info, user);
    }

    @Transactional
    public UserProfileResponse removeProfilePic(String email) {
        User user = getUserByEmail(email);
        UserInfo info = userInfoRepository.findByUser(user).orElse(null);
        if (info != null && info.getProfilePicUrl() != null) {
            if (storageService != null) storageService.deleteImage(info.getProfilePicUrl());
            info.setProfilePicUrl(null);
            userInfoRepository.save(info);
        }
        return buildProfileResponse(user, info, user);
    }

    @Transactional
    public UserProfileResponse updateProfileSettings(String email, UserProfileRequest request) {
        return updateProfile(email, request);
    }

    private UserProfileResponse buildProfileResponse(User user, UserInfo info, User requester) {
        boolean isMe = user.getId().equals(requester.getId());
        boolean isFollowing = followRepository.existsByFollowerIdAndFollowingId(requester.getId(), user.getId());
        boolean isFollowRequested = followRequestRepository.findByRequesterAndTarget(requester, user).isPresent();
        boolean canViewPosts = isMe || !user.isPrivateAccount() || isFollowing;

        int mutualFriendCount = followRepository.countMutualFriends(requester.getId(), user.getId());
        long followerCount = followRepository.countByFollowingId(user.getId());
        long followingCount = followRepository.countByFollowerId(user.getId());

        // SECURITY (M-2): For private accounts, hide profile details (bio/location)
        // from non-followers at the SERVER side — the old response relied on the
        // frontend honoring canViewPosts, so the data leaked to any API caller.
        boolean canViewDetails = canViewPosts;

        return UserProfileResponse.builder()
                .userId(user.getId())
                .email(isMe ? user.getEmail() : null)
                .firstName(info != null ? info.getFirstName() : null)
                .lastName(info != null ? info.getLastName() : null)
                .bio(canViewDetails && info != null ? info.getBio() : null)
                .profilePicUrl(info != null ? info.getProfilePicUrl() : null)
                .followerCount(followerCount)
                .followingCount(followingCount)
                .isPrivateAccount(user.isPrivateAccount())
                .isFollowing(isFollowing)
                .isFollowRequested(isFollowRequested)
                .canViewPosts(canViewPosts)
                .mutualFriendCount(mutualFriendCount)
                .role(user.getRole())
                .city(canViewDetails && info != null ? info.getLocation() : null)
                .build();
    }

    // ─── Follow / Unfollow ────────────────────────────────────

    @Transactional
    @CacheEvict(value = "feed", allEntries = true)
    public FollowToggleResponse toggleFollow(Long targetUserId, String email) {
        User follower = getUserByEmail(email);
        User following = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (follower.getId().equals(following.getId())) {
            throw new RuntimeException("You cannot follow yourself");
        }

        if (isBlocked(follower, following) || isBlocked(following, follower)) {
            throw new RuntimeException("Action not allowed");
        }

        Optional<Follow> existingFollow = followRepository.findByFollowerAndFollowing(follower, following);

        if (existingFollow.isPresent()) {
            followRepository.delete(existingFollow.get());
            long count = followRepository.countByFollowingId(following.getId());
            return new FollowToggleResponse(false, false, count, "Unfollowed successfully");
        }

        if (following.isPrivateAccount()) {
            Optional<FollowRequest> existing = followRequestRepository.findByRequesterAndTarget(follower, following);
            if (existing.isPresent()) {
                if (existing.get().getStatus() == FollowRequestStatus.PENDING) {
                    long count = followRepository.countByFollowingId(following.getId());
                    return new FollowToggleResponse(false, true, count, "Follow request already sent");
                }
                followRequestRepository.delete(existing.get());
            }
            FollowRequest fr = new FollowRequest(follower, following);
            followRequestRepository.save(fr);
            notificationService.createNotification(
                    following, NotificationType.FOLLOW_REQUEST,
                    getDisplayName(follower) + " requested to follow you", follower);
            long count = followRepository.countByFollowingId(following.getId());
            return new FollowToggleResponse(false, true, count, "Follow request sent");
        }

        Follow follow = new Follow();
        follow.setFollower(follower);
        follow.setFollowing(following);
        followRepository.save(follow);

        notificationService.createNotification(
                following, NotificationType.FOLLOW,
                getDisplayName(follower) + " started following you", follower);

        long count = followRepository.countByFollowingId(following.getId());
        return new FollowToggleResponse(true, false, count, "Followed successfully");
    }

    // ─── Follow Requests ──────────────────────────────────────

    @Transactional(readOnly = true)
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
        followRequestRepository.delete(fr);
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
            throw new RuntimeException("Cannot block yourself");
        }

        if (blockRepository.findByBlockerAndBlocked(blocker, blocked).isPresent()) {
            return "User already blocked";
        }

        // Remove follows in both directions
        followRepository.findByFollowerAndFollowing(blocker, blocked)
                .ifPresent(followRepository::delete);
        followRepository.findByFollowerAndFollowing(blocked, blocker)
                .ifPresent(followRepository::delete);

        // Remove follow requests in both directions
        followRequestRepository.findByRequesterAndTarget(blocker, blocked)
                .ifPresent(followRequestRepository::delete);
        followRequestRepository.findByRequesterAndTarget(blocked, blocker)
                .ifPresent(followRequestRepository::delete);

        Block block = new Block();
        block.setBlocker(blocker);
        block.setBlocked(blocked);
        blockRepository.save(block);

        return "User blocked successfully";
    }

    @Transactional
    public String unblockUser(Long targetUserId, String email) {
        User blocker = getUserByEmail(email);
        User blocked = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Block block = blockRepository.findByBlockerAndBlocked(blocker, blocked)
                .orElseThrow(() -> new RuntimeException("User is not blocked"));

        blockRepository.delete(block);
        return "User unblocked successfully";
    }

    public boolean isBlocked(User a, User b) {
        return blockRepository.existsByBlockerAndBlocked(a, b);
    }

    @Transactional(readOnly = true)
    public List<FollowUserResponse> getBlockedUsers(String email) {
        User blocker = getUserByEmail(email);
        return blockRepository.findByBlockerId(blocker.getId()).stream()
                .map(b -> mapToFollowUserResponse(b.getBlocked()))
                .collect(Collectors.toList());
    }

    // ─── Followers / Following ────────────────────────────────

    @Transactional(readOnly = true)
    public List<FollowUserResponse> getFollowers(Long userId, String requesterEmail) {
        // SECURITY: Block check — verify the requester is not blocked by the target user
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (blockRepository.existsByBlockerAndBlocked(target, requester) ||
                blockRepository.existsByBlockerAndBlocked(requester, target)) {
            throw new RuntimeException("Action not allowed");
        }

        // SECURITY (M-2): a private account's social graph is only visible to
        // the account itself and its followers.
        if (!target.getId().equals(requester.getId()) && target.isPrivateAccount()
                && !followRepository.existsByFollowerAndFollowing(requester, target)) {
            throw new RuntimeException("This account is private");
        }

        return followRepository.findFollowersWithUserInfoByFollowingId(userId).stream()
                .map(this::mapToFollowUserResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FollowUserResponse> getFollowing(Long userId, String requesterEmail) {
        // SECURITY: Block check — verify the requester is not blocked by the target user
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (blockRepository.existsByBlockerAndBlocked(target, requester) ||
                blockRepository.existsByBlockerAndBlocked(requester, target)) {
            throw new RuntimeException("Action not allowed");
        }

        // SECURITY (M-2): a private account's social graph is only visible to
        // the account itself and its followers.
        if (!target.getId().equals(requester.getId()) && target.isPrivateAccount()
                && !followRepository.existsByFollowerAndFollowing(requester, target)) {
            throw new RuntimeException("This account is private");
        }

        return followRepository.findByFollowerId(userId).stream()
                .map(follow -> mapToFollowUserResponse(follow.getFollowing()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean isFollowing(Long followerId, Long followingId) {
        User follower = userRepository.findById(followerId).orElse(null);
        User following = userRepository.findById(followingId).orElse(null);
        if (follower == null || following == null) return false;
        return followRepository.findByFollowerAndFollowing(follower, following).isPresent();
    }

    // ─── Network Graph ────────────────────────────────────────

    @Transactional(readOnly = true)
    public NetworkGraphResponse getNetworkGraph(Long targetUserId, String requesterEmail) {
        User requester = getUserByEmail(requesterEmail);
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean canView = target.getId().equals(requester.getId())
                || !target.isPrivateAccount()
                || followRepository.findByFollowerAndFollowing(requester, target).isPresent();
        if (!canView) throw new RuntimeException("Access denied");

        List<User> friends = followRepository.findByFollowerId(target.getId()).stream()
                .map(Follow::getFollowing)
                .collect(Collectors.toList());

        java.util.Set<Long> friendIds = friends.stream().map(User::getId).collect(Collectors.toSet());

        // Batch query: load all followings for all friends in a single query (fixes N+1)
        java.util.Map<Long, java.util.Set<Long>> mutualMap = new java.util.HashMap<>();
        if (!friendIds.isEmpty()) {
            followRepository.findByFollowerIdIn(new java.util.ArrayList<>(friendIds)).forEach(f -> {
                Long mid = f.getFollowing().getId();
                if (!mid.equals(target.getId()) && !friendIds.contains(mid)) {
                    mutualMap.computeIfAbsent(mid, k -> new java.util.HashSet<>()).add(f.getFollower().getId());
                }
            });
        }

        List<NetworkGraphResponse.MutualNodeDTO> mutuals = mutualMap.entrySet().stream()
                .limit(20)
                .map(e -> {
                    User u = userRepository.findById(e.getKey()).orElse(null);
                    if (u == null) return null;
                    return new NetworkGraphResponse.MutualNodeDTO(
                            u.getId(), firstName(u), picUrl(u),
                            List.copyOf(e.getValue()));
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        NetworkGraphResponse.NodeDTO centerNode = new NetworkGraphResponse.NodeDTO(
                target.getId(), firstName(target), picUrl(target));

        List<NetworkGraphResponse.NodeDTO> friendNodes = friends.stream()
                .map(u -> new NetworkGraphResponse.NodeDTO(u.getId(), firstName(u), picUrl(u)))
                .collect(Collectors.toList());

        return new NetworkGraphResponse(centerNode, friendNodes, mutuals);
    }

    // ─── Smart Search ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SearchUserResponse> searchUsers(String requesterEmail, String query,
                                                String location, String bio,
                                                boolean mutualOnly, String sort) {
        User requester = getUserByEmail(requesterEmail);
        List<User> results = userRepository.searchUsers(
                requester.getId(), query, location, bio, PageRequest.of(0, 50));

        Set<Long> myFollowingIds = followRepository.findByFollowerId(requester.getId())
                .stream().map(f -> f.getFollowing().getId()).collect(Collectors.toSet());

        // Batch query: load all followers for all search results in a single query (fixes N+1)
        List<Long> resultIds = results.stream().map(User::getId).collect(Collectors.toList());
        java.util.Map<Long, java.util.Set<Long>> followersOfResult = new java.util.HashMap<>();
        if (!resultIds.isEmpty()) {
            followRepository.findByFollowingIdIn(resultIds).forEach(f ->
                followersOfResult.computeIfAbsent(f.getFollowing().getId(), k -> new java.util.HashSet<>())
                    .add(f.getFollower().getId())
            );
        }

        List<SearchUserResponse> mapped = results.stream().map(u -> {
            // count mutuals: people I follow who also follow this user
            long mutual = followersOfResult.getOrDefault(u.getId(), java.util.Collections.emptySet())
                    .stream().filter(myFollowingIds::contains).count();
            boolean following = myFollowingIds.contains(u.getId());
            String name = u.getUserInfo() != null
                    ? ((u.getUserInfo().getFirstName() != null ? u.getUserInfo().getFirstName() : "") +
                       " " + (u.getUserInfo().getLastName() != null ? u.getUserInfo().getLastName() : "")).trim()
                    : getDisplayName(u);
            if (name.isBlank()) name = getDisplayName(u);
            String loc = u.getUserInfo() != null ? u.getUserInfo().getLocation() : null;
            String userBio = u.getUserInfo() != null ? u.getUserInfo().getBio() : null;
            String pic = u.getUserInfo() != null ? u.getUserInfo().getProfilePicUrl() : null;
            return new SearchUserResponse(u.getId(), name, pic, loc, userBio, (int) mutual, following);
        }).collect(Collectors.toList());

        if (mutualOnly) {
            mapped = mapped.stream().filter(r -> r.getMutualCount() > 0).collect(Collectors.toList());
        }

        if ("mutual".equals(sort)) {
            mapped.sort((a, b) -> Integer.compare(b.getMutualCount(), a.getMutualCount()));
        } else if ("name".equals(sort)) {
            mapped.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        }

        return mapped;
    }

    // ─── Mutual Friends ───────────────────────────────────────

    @Transactional(readOnly = true)
    public List<FollowUserResponse> getMutualFriends(Long targetUserId, String requesterEmail) {
        User requester = getUserByEmail(requesterEmail);
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // SECURITY: Block check — prevent blocked users from seeing mutual friends
        if (blockRepository.existsByBlockerAndBlocked(target, requester) ||
                blockRepository.existsByBlockerAndBlocked(requester, target)) {
            throw new RuntimeException("Action not allowed");
        }

        Set<Long> myFollowingIds = followRepository.findByFollowerId(requester.getId())
                .stream().map(f -> f.getFollowing().getId()).collect(Collectors.toSet());

        return followRepository.findByFollowingId(target.getId()).stream()
                .map(Follow::getFollower)
                .filter(u -> myFollowingIds.contains(u.getId()))
                .map(this::mapToFollowUserResponse)
                .collect(Collectors.toList());
    }

    // ─── Recommendations ──────────────────────────────────────

    @Transactional(readOnly = true)
    public List<RecommendationResponse> getRecommendations(String email) {
        User me = getUserByEmail(email);
        Set<String> myKeywords = extractKeywords(me.getUserInfo() != null ? me.getUserInfo().getBio() : null);
        String myLocation = me.getUserInfo() != null ? me.getUserInfo().getLocation() : null;
        List<User> candidates = userRepository.findRecommendationCandidates(me.getId(), PageRequest.of(0, 100));
        return candidates.stream()
                .map(u -> scoreUser(u, me, myKeywords, myLocation))
                .sorted((a, b) -> Integer.compare(b.getMatchScore(), a.getMatchScore()))
                .limit(25)
                .collect(Collectors.toList());
    }

    private RecommendationResponse scoreUser(User u, User me, Set<String> myKeywords, String myLocation) {
        int score = 20; // Base match score for discovery
        java.util.List<String> reasons = new java.util.ArrayList<>();
        UserInfo info = u.getUserInfo();

        int mutuals = followRepository.countMutualFriends(me.getId(), u.getId());
        if (mutuals > 0) {
            score += Math.min(mutuals * 15, 45);
            reasons.add(mutuals + " mutual friend" + (mutuals > 1 ? "s" : ""));
        }

        String theirLoc = info != null ? info.getLocation() : null;
        if (myLocation != null && theirLoc != null && !myLocation.isBlank() && myLocation.trim().equalsIgnoreCase(theirLoc.trim())) {
            score += 25;
            reasons.add("Same location");
        }

        Set<String> sharedKeywords = extractKeywords(info != null ? info.getBio() : null);
        sharedKeywords.retainAll(myKeywords);
        if (!sharedKeywords.isEmpty()) {
            score += Math.min(sharedKeywords.size() * 10, 30);
            reasons.add("Shared interests");
        }

        if (info != null && info.getProfilePicUrl() != null) score += 5;
        if (reasons.isEmpty()) {
            reasons.add("Suggested for you");
        }

        String name = info != null
                ? ((info.getFirstName() != null ? info.getFirstName() : "") + " "
                   + (info.getLastName() != null ? info.getLastName() : "")).trim()
                : getDisplayName(u);
        if (name.isBlank()) name = getDisplayName(u);

        return new RecommendationResponse(u.getId(), name,
                info != null ? info.getProfilePicUrl() : null,
                theirLoc, info != null ? info.getBio() : null,
                Math.min(score, 98), reasons);
    }

    private Set<String> extractKeywords(String text) {
        if (text == null || text.isBlank()) return new java.util.HashSet<>();
        return java.util.Arrays.stream(text.toLowerCase().split("[^a-z0-9]+"))
                .filter(w -> w.length() > 3)
                .collect(Collectors.toSet());
    }

    // ─── Suggestions ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<FollowUserResponse> getSuggestedUsers(String email) {
        User user = getUserByEmail(email);
        return userRepository.findSuggestedUsers(user.getId(), PageRequest.of(0, 20))
                .stream()
                .map(this::mapToFollowUserResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updatePublicKey(String email, String publicKey) {
        // SECURITY: Basic validation of public key format — must be a valid base64 string
        // of reasonable length for an ECDH P-256 raw public key (~44-512 chars).
        if (publicKey != null && !publicKey.isBlank()) {
            try {
                byte[] decoded = java.util.Base64.getDecoder().decode(publicKey);
                if (decoded.length != 33 && decoded.length != 65 && decoded.length != 91) {
                    throw new RuntimeException("Invalid ECDH P-256 public key length: expected 33, 65, or 91 bytes");
                }
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Public key is not valid base64");
            }
        }
        User user = getUserByEmail(email);
        user.setPublicKey(publicKey);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public PublicKeyResponse getPublicKey(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return new PublicKeyResponse(user.getPublicKey());
    }

    // ─── Helpers ──────────────────────────────────────────────

    private String firstName(User u) {
        return u.getUserInfo() != null ? u.getUserInfo().getFirstName() : null;
    }

    private String picUrl(User u) {
        return u.getUserInfo() != null ? u.getUserInfo().getProfilePicUrl() : null;
    }

    private String getDisplayName(User user) {
        if (user.getUserInfo() != null && user.getUserInfo().getFirstName() != null) {
            return user.getUserInfo().getFirstName() + " " +
                    (user.getUserInfo().getLastName() != null ? user.getUserInfo().getLastName() : "");
        }
        return "User#" + user.getId();
    }

    private FollowUserResponse mapToFollowUserResponse(User user) {
        String name;
        String profilePicUrl = null;
        if (user.getUserInfo() != null && user.getUserInfo().getFirstName() != null) {
            name = (user.getUserInfo().getFirstName() + " " + (user.getUserInfo().getLastName() != null ? user.getUserInfo().getLastName() : "")).trim();
            profilePicUrl = user.getUserInfo().getProfilePicUrl();
        } else {
            name = getDisplayName(user);
        }
        return new FollowUserResponse(user.getId(), name, profilePicUrl);
    }
}
