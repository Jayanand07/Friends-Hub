package com.example.socialmedia.dto;

import java.time.LocalDateTime;

public class StoryResponse {
    private Long storyId;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private int viewerCount;
    private boolean viewedByCurrentUser;

    public StoryResponse() {
    }

    public StoryResponse(Long storyId, String imageUrl, LocalDateTime createdAt,
            LocalDateTime expiresAt, int viewerCount, boolean viewedByCurrentUser) {
        this.storyId = storyId;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.viewerCount = viewerCount;
        this.viewedByCurrentUser = viewedByCurrentUser;
    }

    public Long getStoryId() {
        return storyId;
    }

    public void setStoryId(Long storyId) {
        this.storyId = storyId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public int getViewerCount() {
        return viewerCount;
    }

    public void setViewerCount(int viewerCount) {
        this.viewerCount = viewerCount;
    }

    public boolean isViewedByCurrentUser() {
        return viewedByCurrentUser;
    }

    public void setViewedByCurrentUser(boolean viewedByCurrentUser) {
        this.viewedByCurrentUser = viewedByCurrentUser;
    }
}
