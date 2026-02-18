package com.example.socialmedia.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public class PostResponse {
    private Long id;
    private String content;
    private String imageUrl;
    private String authorName;
    private Long authorId;
    private int likeCount;
    private int commentCount;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    public PostResponse() {
    }

    public PostResponse(Long id, String content, String imageUrl, String authorName, Long authorId, int likeCount,
            int commentCount, LocalDateTime createdAt) {
        this.id = id;
        this.content = content;
        this.imageUrl = imageUrl;
        this.authorName = authorName;
        this.authorId = authorId;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.createdAt = createdAt;
    }

    // Getters and Setters
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static PostResponseBuilder builder() {
        return new PostResponseBuilder();
    }

    public static class PostResponseBuilder {
        private Long id;
        private String content;
        private String imageUrl;
        private String authorName;
        private Long authorId;
        private int likeCount;
        private int commentCount;
        private LocalDateTime createdAt;

        PostResponseBuilder() {
        }

        public PostResponseBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public PostResponseBuilder content(String content) {
            this.content = content;
            return this;
        }

        public PostResponseBuilder imageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        public PostResponseBuilder authorName(String authorName) {
            this.authorName = authorName;
            return this;
        }

        public PostResponseBuilder authorId(Long authorId) {
            this.authorId = authorId;
            return this;
        }

        public PostResponseBuilder likeCount(int likeCount) {
            this.likeCount = likeCount;
            return this;
        }

        public PostResponseBuilder commentCount(int commentCount) {
            this.commentCount = commentCount;
            return this;
        }

        public PostResponseBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public PostResponse build() {
            return new PostResponse(id, content, imageUrl, authorName, authorId, likeCount, commentCount, createdAt);
        }
    }
}
