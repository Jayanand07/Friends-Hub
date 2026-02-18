package com.example.socialmedia.dto;

import java.util.List;

public class StoryUserResponse {
    private Long userId;
    private String name;
    private String profilePicUrl;
    private boolean hasUnviewed;
    private List<StoryResponse> stories;

    public StoryUserResponse() {
    }

    public StoryUserResponse(Long userId, String name, String profilePicUrl,
            boolean hasUnviewed, List<StoryResponse> stories) {
        this.userId = userId;
        this.name = name;
        this.profilePicUrl = profilePicUrl;
        this.hasUnviewed = hasUnviewed;
        this.stories = stories;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public boolean isHasUnviewed() {
        return hasUnviewed;
    }

    public void setHasUnviewed(boolean hasUnviewed) {
        this.hasUnviewed = hasUnviewed;
    }

    public List<StoryResponse> getStories() {
        return stories;
    }

    public void setStories(List<StoryResponse> stories) {
        this.stories = stories;
    }
}
