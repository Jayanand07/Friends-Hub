package com.example.socialmedia.dto;

import java.time.LocalDateTime;

public class ChatGroupMessageDTO {
    private Long id;
    private Long senderId;
    private String senderName;
    private String senderProfilePic;
    private String content;
    private String imageUrl;
    private LocalDateTime createdAt;
    private Boolean isDeleted;
    private String type; // "message", "typing"

    public ChatGroupMessageDTO() {
    }

    public ChatGroupMessageDTO(Long id, Long senderId, String senderName, String senderProfilePic,
            String content, String imageUrl, LocalDateTime createdAt, Boolean isDeleted) {
        this.id = id;
        this.senderId = senderId;
        this.senderName = senderName;
        this.senderProfilePic = senderProfilePic;
        this.content = content;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
        this.isDeleted = isDeleted;
        this.type = "message";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderProfilePic() {
        return senderProfilePic;
    }

    public void setSenderProfilePic(String senderProfilePic) {
        this.senderProfilePic = senderProfilePic;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
