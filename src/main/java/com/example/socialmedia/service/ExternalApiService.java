package com.example.socialmedia.service;

import com.example.socialmedia.entity.Post;
import com.example.socialmedia.entity.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class ExternalApiService {

    private static final Logger log = LoggerFactory.getLogger(ExternalApiService.class);

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
            log.info("External API notified: Post created by user {}", post.getUser().getId());
        } catch (Exception e) {
            log.warn("Failed to notify external API for post creation: {}", e.getMessage());
        }
    }

    public void notifyUserRegistered(User user) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("title", "New User Registered");
            payload.put("body", "New user registered");
            payload.put("userId", user.getId());

            restTemplate.postForObject(EXTERNAL_API_URL, payload, String.class);
            log.info("External API notified: User registered with ID {}", user.getId());
        } catch (Exception e) {
            log.warn("Failed to notify external API for user registration: {}", e.getMessage());
        }
    }
}
