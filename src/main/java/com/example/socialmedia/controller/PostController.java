package com.example.socialmedia.controller;

import com.example.socialmedia.dto.PostRequest;
import com.example.socialmedia.dto.PostResponse;
import com.example.socialmedia.dto.CommentRequest;
import com.example.socialmedia.dto.CommentResponse;
import com.example.socialmedia.dto.MessageResponse;
import com.example.socialmedia.service.PostService;
import com.example.socialmedia.service.SupabaseStorageService;
import org.springframework.security.core.Authentication;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;
    private final SupabaseStorageService storageService;

    private static final org.slf4j.Logger log =
        org.slf4j.LoggerFactory.getLogger(PostController.class);

    public PostController(PostService postService, SupabaseStorageService storageService) {
        this.postService = postService;
        this.storageService = storageService;
    }

    @PostMapping("/upload-image")
    public ResponseEntity<?> uploadImage(@RequestParam("image") MultipartFile image,
            Authentication authentication) {
        // SECURITY: Track which user uploaded this image
        String userEmail = authentication != null ? authentication.getName() : "anonymous";
        log.info("Image upload initiated by user: {}", userEmail);
        try {
            String imageUrl = storageService.uploadImage(image);
            log.info("Image upload successful for user: {}, url: {}", userEmail, imageUrl);
            return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
        } catch (IllegalArgumentException e) {
            log.warn("Image upload rejected for user {}: {}", userEmail, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Image upload failed for user {}: {}", userEmail, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("message", "Upload failed: " + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<PostResponse> createPost(
            @Valid @RequestBody PostRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(postService.createPost(request, authentication.getName()));
    }

    @GetMapping
    public ResponseEntity<Page<PostResponse>> getAllPosts(Pageable pageable, Authentication authentication) {
        return ResponseEntity.ok(postService.getAllPosts(pageable, authentication.getName()));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<PostResponse>> getPostsByUser(
            @PathVariable Long userId,
            Pageable pageable,
            Authentication authentication) {
        return ResponseEntity.ok(postService.getPostsByUser(userId, pageable, authentication.getName()));
    }

    @GetMapping("/{postId}/comments")
    public ResponseEntity<Page<CommentResponse>> getCommentsByPost(@PathVariable Long postId, Pageable pageable,
            Authentication authentication) {
        return ResponseEntity.ok(postService.getCommentsByPost(postId, pageable, authentication.getName()));
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<MessageResponse> likePost(@PathVariable Long postId, Authentication authentication) {
        String message = postService.toggleLike(postId, authentication.getName());
        return ResponseEntity.ok(new MessageResponse(message));
    }

    @PostMapping("/{postId}/save")
    public ResponseEntity<MessageResponse> savePost(@PathVariable Long postId, Authentication authentication) {
        String message = postService.toggleSavePost(postId, authentication.getName());
        return ResponseEntity.ok(new MessageResponse(message));
    }

    @GetMapping("/saved")
    public ResponseEntity<Page<PostResponse>> getSavedPosts(Pageable pageable, Authentication authentication) {
        return ResponseEntity.ok(postService.getSavedPosts(authentication.getName(), pageable));
    }

    @PostMapping("/{postId}/comment")
    public ResponseEntity<MessageResponse> addComment(
            @PathVariable Long postId,
            @Valid @RequestBody CommentRequest request,
            Authentication authentication) {
        postService.addComment(postId, request, authentication.getName());
        return ResponseEntity.ok(new MessageResponse("Comment added successfully"));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<MessageResponse> deletePost(@PathVariable Long postId, Authentication authentication) {
        postService.deletePost(postId, authentication.getName());
        return ResponseEntity.ok(new MessageResponse("Post deleted successfully"));
    }
}
