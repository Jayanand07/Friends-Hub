package com.example.socialmedia.controller;

import com.example.socialmedia.dto.FollowToggleResponse;
import com.example.socialmedia.dto.FollowUserResponse;
import com.example.socialmedia.dto.FriendRequestAnalyticsResponse;
import com.example.socialmedia.dto.MessageResponse;
import com.example.socialmedia.dto.NetworkGraphResponse;
import com.example.socialmedia.dto.PublicKeyRequest;
import com.example.socialmedia.dto.PublicKeyResponse;
import com.example.socialmedia.dto.RecommendationResponse;
import com.example.socialmedia.dto.SearchUserResponse;
import com.example.socialmedia.dto.UserProfileRequest;
import com.example.socialmedia.dto.UserProfileResponse;
import com.example.socialmedia.service.FriendRequestAnalyticsService;
import com.example.socialmedia.service.SupabaseStorageService;
import com.example.socialmedia.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final SupabaseStorageService storageService;
    private final FriendRequestAnalyticsService analyticsService;

    public UserController(UserService userService, SupabaseStorageService storageService,
                          FriendRequestAnalyticsService analyticsService) {
        this.userService = userService;
        this.storageService = storageService;
        this.analyticsService = analyticsService;
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(Authentication authentication) {
        return ResponseEntity.ok(userService.getProfile(authentication.getName()));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> getProfileById(
            @PathVariable Long userId,
            Authentication authentication) {
        return ResponseEntity.ok(userService.getOtherProfile(userId, authentication.getName()));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @RequestBody UserProfileRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(userService.updateProfile(authentication.getName(), request));
    }

    @PutMapping("/profile/settings")
    public ResponseEntity<UserProfileResponse> updateProfileSettings(
            @RequestBody UserProfileRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(userService.updateProfileSettings(authentication.getName(), request));
    }

    @PostMapping("/profile/picture")
    public ResponseEntity<?> uploadProfilePicture(
            @RequestParam("image") MultipartFile image,
            Authentication authentication) {
        try {
            String imageUrl = storageService.uploadImage(image);
            userService.updateProfilePic(authentication.getName(), imageUrl);
            return ResponseEntity.ok(Map.of("profilePicUrl", imageUrl));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Upload failed: " + e.getMessage()));
        }
    }

    @DeleteMapping("/profile/picture")
    public ResponseEntity<UserProfileResponse> removeProfilePicture(Authentication authentication) {
        return ResponseEntity.ok(userService.removeProfilePic(authentication.getName()));
    }

    // ─── Follow ───────────────────────────────────────────────

    @PostMapping("/{userId}/follow")
    public ResponseEntity<FollowToggleResponse> toggleFollow(@PathVariable Long userId, Authentication authentication) {
        return ResponseEntity.ok(userService.toggleFollow(userId, authentication.getName()));
    }

    @GetMapping("/{userId}/followers")
    public ResponseEntity<List<FollowUserResponse>> getFollowers(@PathVariable Long userId,
            Authentication authentication) {
        return ResponseEntity.ok(userService.getFollowers(userId, authentication.getName()));
    }

    @GetMapping("/{userId}/following")
    public ResponseEntity<List<FollowUserResponse>> getFollowing(@PathVariable Long userId,
            Authentication authentication) {
        return ResponseEntity.ok(userService.getFollowing(userId, authentication.getName()));
    }

    // ─── Privacy ──────────────────────────────────────────────

    @PostMapping("/private/toggle")
    public ResponseEntity<?> togglePrivateAccount(Authentication authentication) {
        return ResponseEntity.ok(userService.togglePrivateAccount(authentication.getName()));
    }

    // ─── Follow Requests ──────────────────────────────────────

    @GetMapping("/follow-requests")
    public ResponseEntity<List<FollowUserResponse>> getFollowRequests(Authentication authentication) {
        return ResponseEntity.ok(userService.getPendingFollowRequests(authentication.getName()));
    }

    @PostMapping("/follow-request/{requestId}/accept")
    public ResponseEntity<MessageResponse> acceptFollowRequest(@PathVariable Long requestId,
            Authentication authentication) {
        return ResponseEntity
                .ok(new MessageResponse(userService.acceptFollowRequest(requestId, authentication.getName())));
    }

    @PostMapping("/follow-request/{requestId}/reject")
    public ResponseEntity<MessageResponse> rejectFollowRequest(@PathVariable Long requestId,
            Authentication authentication) {
        return ResponseEntity
                .ok(new MessageResponse(userService.rejectFollowRequest(requestId, authentication.getName())));
    }

    @PostMapping("/follow-request/user/{requesterId}/accept")
    public ResponseEntity<MessageResponse> acceptFollowRequestFromUser(@PathVariable Long requesterId,
            Authentication authentication) {
        return ResponseEntity.ok(
                new MessageResponse(userService.acceptFollowRequestFromUser(requesterId, authentication.getName())));
    }

    @PostMapping("/follow-request/user/{requesterId}/reject")
    public ResponseEntity<MessageResponse> rejectFollowRequestFromUser(@PathVariable Long requesterId,
            Authentication authentication) {
        return ResponseEntity.ok(
                new MessageResponse(userService.rejectFollowRequestFromUser(requesterId, authentication.getName())));
    }

    // ─── Block ────────────────────────────────────────────────

    @PostMapping("/{userId}/block")
    public ResponseEntity<MessageResponse> blockUser(@PathVariable Long userId, Authentication authentication) {
        return ResponseEntity.ok(new MessageResponse(userService.blockUser(userId, authentication.getName())));
    }

    @PostMapping("/{userId}/unblock")
    public ResponseEntity<MessageResponse> unblockUser(@PathVariable Long userId, Authentication authentication) {
        return ResponseEntity.ok(new MessageResponse(userService.unblockUser(userId, authentication.getName())));
    }

    @GetMapping("/blocked")
    public ResponseEntity<List<FollowUserResponse>> getBlockedUsers(Authentication authentication) {
        return ResponseEntity.ok(userService.getBlockedUsers(authentication.getName()));
    }

    @GetMapping("/recommendations")
    public ResponseEntity<List<RecommendationResponse>> getRecommendations(Authentication authentication) {
        return ResponseEntity.ok(userService.getRecommendations(authentication.getName()));
    }

    @GetMapping("/analytics/friend-requests")
    public ResponseEntity<FriendRequestAnalyticsResponse> getFriendRequestAnalytics(Authentication authentication) {
        return ResponseEntity.ok(analyticsService.getAnalytics(authentication.getName()));
    }

    @GetMapping("/search")
    public ResponseEntity<List<SearchUserResponse>> searchUsers(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "") String location,
            @RequestParam(defaultValue = "") String bio,
            @RequestParam(defaultValue = "false") boolean mutualOnly,
            @RequestParam(defaultValue = "relevance") String sort,
            Authentication authentication) {
        return ResponseEntity.ok(userService.searchUsers(
                authentication.getName(), query, location, bio, mutualOnly, sort));
    }

    @GetMapping("/{userId}/mutuals")
    public ResponseEntity<List<FollowUserResponse>> getMutualFriends(
            @PathVariable Long userId,
            Authentication authentication) {
        return ResponseEntity.ok(userService.getMutualFriends(userId, authentication.getName()));
    }

    @GetMapping("/{userId}/network")
    public ResponseEntity<NetworkGraphResponse> getNetworkGraph(
            @PathVariable Long userId,
            Authentication authentication) {
        return ResponseEntity.ok(userService.getNetworkGraph(userId, authentication.getName()));
    }

    @GetMapping("/suggestions")
    public ResponseEntity<List<FollowUserResponse>> getSuggestedUsers(Authentication authentication) {
        return ResponseEntity.ok(userService.getSuggestedUsers(authentication.getName()));
    }

    @PutMapping("/me/public-key")
    public ResponseEntity<MessageResponse> updatePublicKey(
            @RequestBody PublicKeyRequest request,
            Authentication authentication) {
        userService.updatePublicKey(authentication.getName(), request.getPublicKey());
        return ResponseEntity.ok(new MessageResponse("Public key updated"));
    }

    @GetMapping("/{userId}/public-key")
    public ResponseEntity<PublicKeyResponse> getPublicKey(
            @PathVariable Long userId,
            Authentication authentication) {
        return ResponseEntity.ok(userService.getPublicKey(userId));
    }
}
