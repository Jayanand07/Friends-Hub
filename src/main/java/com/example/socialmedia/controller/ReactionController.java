package com.example.socialmedia.controller;

import com.example.socialmedia.service.ReactionService;

import jakarta.validation.constraints.NotBlank;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reactions")
@Validated
public class ReactionController {

    private final ReactionService reactionService;

    public ReactionController(ReactionService reactionService) {
        this.reactionService = reactionService;
    }

    @PostMapping
    public ResponseEntity<?> addReaction(
            @RequestBody Map<String, Object> body,
            Authentication authentication) {
        if (body == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Request body is required"));
        }
        if (!(body.get("targetType") instanceof String targetType) || targetType.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "targetType must be a non-blank string"));
        }
        if (body.get("targetId") == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "targetId is required"));
        }
        Long targetId = Long.valueOf(body.get("targetId").toString());
        String emoji = (String) body.get("emoji");
        String gifUrl = (String) body.get("gifUrl");
        return ResponseEntity
                .ok(reactionService.addReaction(authentication.getName(), targetType, targetId, emoji, gifUrl));
    }

    @DeleteMapping("/{targetType}/{targetId}")
    public ResponseEntity<?> removeReaction(
            @PathVariable @NotBlank(message = "Target type is required") String targetType,
            @PathVariable Long targetId,
            Authentication authentication) {
        reactionService.removeReaction(authentication.getName(), targetType, targetId);
        return ResponseEntity.ok(Map.of("message", "Reaction removed"));
    }

    @GetMapping("/{targetType}/{targetId}")
    public ResponseEntity<List<Map<String, Object>>> getReactions(
            @PathVariable @NotBlank(message = "Target type is required") String targetType,
            @PathVariable Long targetId) {
        return ResponseEntity.ok(reactionService.getReactions(targetType, targetId));
    }
}
