package com.example.socialmedia.dto;

public class PostRequest {
    private String content;
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
