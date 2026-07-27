package com.example.socialmedia.dto;

import java.time.LocalDateTime;

public class AdminActionLogResponse {
    private Long id;
    private Long adminId;
    private String adminName;
    private String actionType;
    private Long targetUserId;
    private Long targetPostId;
    private String description;
    private LocalDateTime createdAt;

    public AdminActionLogResponse() {
    }

    public AdminActionLogResponse(Long id, Long adminId, String adminName, String actionType,
            Long targetUserId, Long targetPostId, String description, LocalDateTime createdAt) {
        this.id = id;
        this.adminId = adminId;
        this.adminName = adminName;
        this.actionType = actionType;
        this.targetUserId = targetUserId;
        this.targetPostId = targetPostId;
        this.description = description;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAdminId() { return adminId; }
    public void setAdminId(Long adminId) { this.adminId = adminId; }
    public String getAdminName() { return adminName; }
    public void setAdminName(String adminName) { this.adminName = adminName; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public Long getTargetUserId() { return targetUserId; }
    public void setTargetUserId(Long targetUserId) { this.targetUserId = targetUserId; }
    public Long getTargetPostId() { return targetPostId; }
    public void setTargetPostId(Long targetPostId) { this.targetPostId = targetPostId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
