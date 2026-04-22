package com.example.socialmedia.controller;

import com.example.socialmedia.dto.PostRequest;
import com.example.socialmedia.dto.PostResponse;
import com.example.socialmedia.dto.CommentRequest;
import com.example.socialmedia.dto.CommentResponse;
import com.example.socialmedia.dto.MessageResponse;
import com.example.socialmedia.service.PostService;
import com.example.socialmedia.service.SupabaseStorageService;
import org.springframework.security.core.Authentication;

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

    public PostController(PostService postService, SupabaseStorageService storageService) {
        this.postService = postService;
        this.storageService = storageService;
    }

    @PostMapping("/upload-image")
    public ResponseEntity<?> uploadImage(@RequestParam("image") MultipartFile image) {
        try {
            String imageUrl = storageService.uploadImage(image);
            return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Upload failed: " + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<PostResponse> createPost(
            @RequestBody PostRequest request,
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
    public ResponseEntity<Page<CommentResponse>> getCommentsByPost(@PathVariable Long postId, Pageable pageable) {
        return ResponseEntity.ok(postService.getCommentsByPost(postId, pageable));
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<MessageResponse> likePost(@PathVariable Long postId, Authentication authentication) {
        String message = postService.toggleLike(postId, authentication.getName());
        return ResponseEntity.ok(new MessageResponse(message));
    }

    @PostMapping("/{postId}/comment")
    public ResponseEntity<MessageResponse> addComment(
            @PathVariable Long postId,
            @RequestBody CommentRequest request,
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
