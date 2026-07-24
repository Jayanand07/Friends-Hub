package com.example.socialmedia.dto.kafka;

import java.time.Instant;

public class ActivityEvent {
    private Long userId;
    private String action;    // "POST_VIEW", "STORY_VIEW", "LOGIN"
    private Long resourceId;
    private Instant timestamp;

    public ActivityEvent() {}

    public ActivityEvent(Long userId, String action, Long resourceId, Instant timestamp) {
        this.userId = userId;
        this.action = action;
        this.resourceId = resourceId;
        this.timestamp = timestamp;
    }

    public Long getUserId() { return userId; }
    public String getAction() { return action; }
    public Long getResourceId() { return resourceId; }
    public Instant getTimestamp() { return timestamp; }
}
