package com.example.socialmedia.entity;

import jakarta.persistence.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Enumerated(EnumType.STRING)
    private Role role = Role.ROLE_USER;

    private String verificationToken;

    private LocalDateTime tokenExpiry;

    private String passwordResetToken;

    private LocalDateTime resetTokenExpiry;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private boolean isPrivateAccount = false;

    @Column(nullable = false)
    private boolean allowStoryViewByFollowersOnly = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProfileVisibility profileVisibility = ProfileVisibility.PUBLIC;

    // Relationships

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserInfo userInfo;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Post> posts;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Comment> comments;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Like> likes;

    // No-args constructor
    public User() {
    }

    // All-args constructor
    public User(Long id, String email, String password, VerificationStatus verificationStatus, Role role,
            String verificationToken, LocalDateTime tokenExpiry, String passwordResetToken,
            LocalDateTime resetTokenExpiry,
            LocalDateTime createdAt, LocalDateTime updatedAt,
            boolean isPrivateAccount, boolean allowStoryViewByFollowersOnly, ProfileVisibility profileVisibility,
            UserInfo userInfo, List<Post> posts, List<Comment> comments, List<Like> likes) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.verificationStatus = verificationStatus;
        this.role = role;
        this.verificationToken = verificationToken;
        this.tokenExpiry = tokenExpiry;
        this.passwordResetToken = passwordResetToken;
        this.resetTokenExpiry = resetTokenExpiry;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isPrivateAccount = isPrivateAccount;
        this.allowStoryViewByFollowersOnly = allowStoryViewByFollowersOnly;
        this.profileVisibility = profileVisibility;
        this.userInfo = userInfo;
        this.posts = posts;
        this.comments = comments;
        this.likes = likes;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    public void setVerificationToken(String verificationToken) {
        this.verificationToken = verificationToken;
    }

    public LocalDateTime getTokenExpiry() {
        return tokenExpiry;
    }

    public void setTokenExpiry(LocalDateTime tokenExpiry) {
        this.tokenExpiry = tokenExpiry;
    }

    public String getPasswordResetToken() {
        return passwordResetToken;
    }

    public void setPasswordResetToken(String passwordResetToken) {
        this.passwordResetToken = passwordResetToken;
    }

    public LocalDateTime getResetTokenExpiry() {
        return resetTokenExpiry;
    }

    public void setResetTokenExpiry(LocalDateTime resetTokenExpiry) {
        this.resetTokenExpiry = resetTokenExpiry;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    public List<Post> getPosts() {
        return posts;
    }

    public void setPosts(List<Post> posts) {
        this.posts = posts;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public List<Like> getLikes() {
        return likes;
    }

    public void setLikes(List<Like> likes) {
        this.likes = likes;
    }

    public boolean isPrivateAccount() {
        return isPrivateAccount;
    }

    public void setPrivateAccount(boolean privateAccount) {
        isPrivateAccount = privateAccount;
    }

    public boolean isAllowStoryViewByFollowersOnly() {
        return allowStoryViewByFollowersOnly;
    }

    public void setAllowStoryViewByFollowersOnly(boolean allowStoryViewByFollowersOnly) {
        this.allowStoryViewByFollowersOnly = allowStoryViewByFollowersOnly;
    }

    public ProfileVisibility getProfileVisibility() {
        return profileVisibility;
    }

    public void setProfileVisibility(ProfileVisibility profileVisibility) {
        this.profileVisibility = profileVisibility;
    }

    // Builder
    public static UserBuilder builder() {
        return new UserBuilder();
    }

    public static class UserBuilder {
        private Long id;
        private String email;
        private String password;
        private VerificationStatus verificationStatus = VerificationStatus.PENDING;
        private Role role = Role.ROLE_USER;
        private String verificationToken;
        private LocalDateTime tokenExpiry;
        private String passwordResetToken;
        private LocalDateTime resetTokenExpiry;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private boolean isPrivateAccount = false;
        private boolean allowStoryViewByFollowersOnly = true;
        private ProfileVisibility profileVisibility = ProfileVisibility.PUBLIC;
        private UserInfo userInfo;
        private List<Post> posts;
        private List<Comment> comments;
        private List<Like> likes;

        UserBuilder() {
        }

        public UserBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public UserBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserBuilder password(String password) {
            this.password = password;
            return this;
        }

        public UserBuilder verificationStatus(VerificationStatus verificationStatus) {
            this.verificationStatus = verificationStatus;
            return this;
        }

        public UserBuilder role(Role role) {
            this.role = role;
            return this;
        }

        public UserBuilder verificationToken(String verificationToken) {
            this.verificationToken = verificationToken;
            return this;
        }

        public UserBuilder tokenExpiry(LocalDateTime tokenExpiry) {
            this.tokenExpiry = tokenExpiry;
            return this;
        }

        public UserBuilder passwordResetToken(String passwordResetToken) {
            this.passwordResetToken = passwordResetToken;
            return this;
        }

        public UserBuilder resetTokenExpiry(LocalDateTime resetTokenExpiry) {
            this.resetTokenExpiry = resetTokenExpiry;
            return this;
        }

        public UserBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public UserBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public UserBuilder isPrivateAccount(boolean isPrivateAccount) {
            this.isPrivateAccount = isPrivateAccount;
            return this;
        }

        public UserBuilder allowStoryViewByFollowersOnly(boolean allowStoryViewByFollowersOnly) {
            this.allowStoryViewByFollowersOnly = allowStoryViewByFollowersOnly;
            return this;
        }

        public UserBuilder profileVisibility(ProfileVisibility profileVisibility) {
            this.profileVisibility = profileVisibility;
            return this;
        }

        public UserBuilder userInfo(UserInfo userInfo) {
            this.userInfo = userInfo;
            return this;
        }

        public UserBuilder posts(List<Post> posts) {
            this.posts = posts;
            return this;
        }

        public UserBuilder comments(List<Comment> comments) {
            this.comments = comments;
            return this;
        }

        public UserBuilder likes(List<Like> likes) {
            this.likes = likes;
            return this;
        }

        public User build() {
            return new User(id, email, password, verificationStatus, role, verificationToken, tokenExpiry,
                    passwordResetToken, resetTokenExpiry,
                    createdAt, updatedAt, isPrivateAccount, allowStoryViewByFollowersOnly, profileVisibility,
                    userInfo, posts, comments, likes);
        }
    }
}
