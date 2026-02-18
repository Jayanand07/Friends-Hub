package com.example.socialmedia.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "user_info")
public class UserInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;
    private String lastName;
    private String bio;
    private String profilePicUrl;
    private String website;
    private String location;
    private String gender;
    private String dob;
    private String phone;

    @Enumerated(EnumType.STRING)
    private ProfileVisibility profileVisibility = ProfileVisibility.PUBLIC;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    public UserInfo() {
    }

    public UserInfo(Long id, String firstName, String lastName, String bio, String profilePicUrl, User user) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.bio = bio;
        this.profilePicUrl = profilePicUrl;
        this.user = user;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getProfilePicUrl() {
        return profilePicUrl;
    }

    public void setProfilePicUrl(String profilePicUrl) {
        this.profilePicUrl = profilePicUrl;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public ProfileVisibility getProfileVisibility() {
        return profileVisibility;
    }

    public void setProfileVisibility(ProfileVisibility profileVisibility) {
        this.profileVisibility = profileVisibility;
    }

    public static UserInfoBuilder builder() {
        return new UserInfoBuilder();
    }

    public static class UserInfoBuilder {
        private Long id;
        private String firstName;
        private String lastName;
        private String bio;
        private String profilePicUrl;
        private User user;

        UserInfoBuilder() {
        }

        public UserInfoBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public UserInfoBuilder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public UserInfoBuilder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public UserInfoBuilder bio(String bio) {
            this.bio = bio;
            return this;
        }

        public UserInfoBuilder profilePicUrl(String profilePicUrl) {
            this.profilePicUrl = profilePicUrl;
            return this;
        }

        public UserInfoBuilder user(User user) {
            this.user = user;
            return this;
        }

        public UserInfo build() {
            return new UserInfo(id, firstName, lastName, bio, profilePicUrl, user);
        }
    }
}
