package com.example.socialmedia.controller;

import com.example.socialmedia.dto.MessageResponse;
import com.example.socialmedia.service.PostService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final PostService postService;

    public CommentController(PostService postService) {
        this.postService = postService;
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<MessageResponse> deleteComment(@PathVariable Long commentId, Authentication authentication) {
        postService.deleteComment(commentId, authentication.getName());
        return ResponseEntity.ok(new MessageResponse("Comment deleted successfully"));
    }
}
