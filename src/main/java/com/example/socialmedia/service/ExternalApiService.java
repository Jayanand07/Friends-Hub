package com.example.socialmedia.service;

import com.example.socialmedia.entity.Post;
import com.example.socialmedia.entity.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Event logging service for internal processing.
 * 
 * SECURITY FIX (H-5): Removed all external HTTP calls that were leaking
 * user-generated content (post bodies, user IDs) to jsonplaceholder.typicode.com.
 * Now logs events internally only. Use Kafka EventProducerService for async processing.
 */
@Service
public class ExternalApiService {

    private static final Logger log = LoggerFactory.getLogger(ExternalApiService.class);

    public ExternalApiService() {
        // No external dependencies needed
    }

    public void notifyPostCreated(Post post) {
        log.debug("Post created event: postId={}, userId={}", post.getId(), post.getUser().getId());
    }

    public void notifyUserRegistered(User user) {
        log.debug("User registered event: userId={}", user.getId());
    }
}
