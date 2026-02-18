package com.example.socialmedia.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import com.example.socialmedia.entity.Role;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@NoArgsConstructor
public class UserProfileResponse {
    private Long userId;
    private String email;
    private Role role;
    private String firstName;
    private String lastName;
    private String bio;
    private String city;
    private String profilePicUrl;
    private long followerCount;
    private long followingCount;

    @JsonProperty("isPrivateAccount")
    private boolean privateAccount;

    @JsonProperty("isFollowing")
    private boolean following;

    @JsonProperty("isFollowRequested")
    private boolean followRequested;

    private boolean canViewPosts;

    public UserProfileResponse(Long userId, String email, Role role, String firstName, String lastName, String bio,
            String city, String profilePicUrl, long followerCount, long followingCount, boolean privateAccount,
            boolean following, boolean followRequested, boolean canViewPosts) {
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.firstName = firstName;
        this.lastName = lastName;
        this.bio = bio;
        this.city = city;
        this.profilePicUrl = profilePicUrl;
        this.followerCount = followerCount;
        this.followingCount = followingCount;
        this.privateAccount = privateAccount;
        this.following = following;
        this.followRequested = followRequested;
        this.canViewPosts = canViewPosts;
    }

    public static UserProfileResponseBuilder builder() {
        return new UserProfileResponseBuilder();
    }

    public static class UserProfileResponseBuilder {
        private Long userId;
        private String email;
        private Role role;
        private String firstName;
        private String lastName;
        private String bio;
        private String city;
        private String profilePicUrl;
        private long followerCount;
        private long followingCount;
        private boolean privateAccount;
        private boolean following;
        private boolean followRequested;
        private boolean canViewPosts;

        UserProfileResponseBuilder() {
        }

        public UserProfileResponseBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public UserProfileResponseBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserProfileResponseBuilder role(Role role) {
            this.role = role;
            return this;
        }

        public UserProfileResponseBuilder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public UserProfileResponseBuilder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public UserProfileResponseBuilder bio(String bio) {
            this.bio = bio;
            return this;
        }

        public UserProfileResponseBuilder city(String city) {
            this.city = city;
            return this;
        }

        public UserProfileResponseBuilder profilePicUrl(String profilePicUrl) {
            this.profilePicUrl = profilePicUrl;
            return this;
        }

        public UserProfileResponseBuilder followerCount(long followerCount) {
            this.followerCount = followerCount;
            return this;
        }

        public UserProfileResponseBuilder followingCount(long followingCount) {
            this.followingCount = followingCount;
            return this;
        }

        public UserProfileResponseBuilder isPrivateAccount(boolean privateAccount) {
            this.privateAccount = privateAccount;
            return this;
        }

        public UserProfileResponseBuilder isFollowing(boolean following) {
            this.following = following;
            return this;
        }

        public UserProfileResponseBuilder isFollowRequested(boolean followRequested) {
            this.followRequested = followRequested;
            return this;
        }

        public UserProfileResponseBuilder canViewPosts(boolean canViewPosts) {
            this.canViewPosts = canViewPosts;
            return this;
        }

        public UserProfileResponse build() {
            return new UserProfileResponse(userId, email, role, firstName, lastName, bio, city, profilePicUrl,
                    followerCount, followingCount, privateAccount, following, followRequested, canViewPosts);
        }
    }
}
