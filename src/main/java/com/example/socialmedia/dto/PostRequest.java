package com.example.socialmedia.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class PostRequest {
    @Size(max = 5000, message = "Post content cannot exceed 5000 characters")
    private String content;

    @Pattern(regexp = "^(https?://.*)?$", message = "Image URL must be a valid http/https URL")
    @Size(max = 2048, message = "Image URL cannot exceed 2048 characters")
    private String imageUrl;

    public PostRequest() {
    }

    public PostRequest(String content, String imageUrl) {
        this.content = content;
        this.imageUrl = imageUrl;
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

    public static PostRequestBuilder builder() {
        return new PostRequestBuilder();
    }

    public static class PostRequestBuilder {
        private String content;
        private String imageUrl;

        PostRequestBuilder() {
        }

        public PostRequestBuilder content(String content) {
            this.content = content;
            return this;
        }

        public PostRequestBuilder imageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        public PostRequest build() {
            return new PostRequest(content, imageUrl);
        }
    }
}
