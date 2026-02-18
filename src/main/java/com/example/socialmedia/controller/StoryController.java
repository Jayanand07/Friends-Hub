package com.example.socialmedia.controller;

import com.example.socialmedia.dto.FollowUserResponse;
import com.example.socialmedia.dto.MessageResponse;
import com.example.socialmedia.dto.StoryResponse;
import com.example.socialmedia.dto.StoryUserResponse;
import com.example.socialmedia.service.StoryService;
import com.example.socialmedia.service.SupabaseStorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stories")
public class StoryController {

    private final StoryService storyService;
    private final SupabaseStorageService storageService;

    public StoryController(StoryService storyService, SupabaseStorageService storageService) {
        this.storyService = storyService;
        this.storageService = storageService;
    }

    @PostMapping
    public ResponseEntity<?> uploadStory(@RequestParam("image") MultipartFile image,
            Authentication authentication) {
        try {
            String imageUrl = storageService.uploadImage(image);
            StoryResponse story = storyService.uploadStory(authentication.getName(), imageUrl);
            return ResponseEntity.ok(story);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Upload failed: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<StoryUserResponse>> getActiveStories(Authentication authentication) {
        return ResponseEntity.ok(storyService.getActiveStories(authentication.getName()));
    }

    @PostMapping("/{storyId}/view")
    public ResponseEntity<MessageResponse> viewStory(@PathVariable Long storyId, Authentication authentication) {
        storyService.viewStory(storyId, authentication.getName());
        return ResponseEntity.ok(new MessageResponse("Story viewed"));
    }

    @GetMapping("/{storyId}/viewers")
    public ResponseEntity<List<FollowUserResponse>> getStoryViewers(@PathVariable Long storyId) {
        return ResponseEntity.ok(storyService.getStoryViewers(storyId));
    }
}
