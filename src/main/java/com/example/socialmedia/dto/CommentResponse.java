package com.example.socialmedia.dto;

import java.time.LocalDateTime;

public class CommentResponse {
    private Long id;
    private String content;
    private String commenterName;
    private LocalDateTime createdAt;

    public CommentResponse() {
    }

    public CommentResponse(Long id, String content, String commenterName, LocalDateTime createdAt) {
        this.id = id;
        this.content = content;
        this.commenterName = commenterName;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCommenterName() {
        return commenterName;
    }

    public void setCommenterName(String commenterName) {
        this.commenterName = commenterName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
