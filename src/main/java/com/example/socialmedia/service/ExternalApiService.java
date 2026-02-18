package com.example.socialmedia.service;

import com.example.socialmedia.entity.Post;
import com.example.socialmedia.entity.User;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class ExternalApiService {

    private final RestTemplate restTemplate;

    private static final String EXTERNAL_API_URL = "https://jsonplaceholder.typicode.com/posts";

    public ExternalApiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void notifyPostCreated(Post post) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("title", "New Post Created");
            payload.put("body", post.getContent());
            payload.put("userId", post.getUser().getId());

            restTemplate.postForObject(EXTERNAL_API_URL, payload, String.class);
            System.out.println("External API notified: Post created by user " + post.getUser().getEmail());
        } catch (Exception e) {
            System.err.println("Failed to notify external API for post creation: " + e.getMessage());
        }
    }

    public void notifyUserRegistered(User user) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("title", "New User Registered");
            payload.put("body", "User registered with email: " + user.getEmail());
            payload.put("userId", user.getId());

            restTemplate.postForObject(EXTERNAL_API_URL, payload, String.class);
            System.out.println("External API notified: User registered - " + user.getEmail());
        } catch (Exception e) {
            System.err.println("Failed to notify external API for user registration: " + e.getMessage());
        }
    }
}
