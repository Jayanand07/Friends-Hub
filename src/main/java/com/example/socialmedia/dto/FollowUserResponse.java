package com.example.socialmedia.dto;

public class FollowUserResponse {
    private Long id;
    private String name;
    private String profilePicUrl;

    public FollowUserResponse() {
    }

    public FollowUserResponse(Long id, String name, String profilePicUrl) {
        this.id = id;
        this.name = name;
        this.profilePicUrl = profilePicUrl;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProfilePicUrl() {
        return profilePicUrl;
    }

    public void setProfilePicUrl(String profilePicUrl) {
        this.profilePicUrl = profilePicUrl;
    }
}
