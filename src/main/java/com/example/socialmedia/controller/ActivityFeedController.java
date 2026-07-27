package com.example.socialmedia.controller;

import com.example.socialmedia.dto.ActivityFeedItem;
import com.example.socialmedia.service.ActivityFeedService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/activity")
@Validated
public class ActivityFeedController {

    private final ActivityFeedService activityFeedService;

    public ActivityFeedController(ActivityFeedService activityFeedService) {
        this.activityFeedService = activityFeedService;
    }

    @GetMapping("/feed")
    public ResponseEntity<List<ActivityFeedItem>> getFeed(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            Authentication authentication) {
        return ResponseEntity.ok(activityFeedService.getFeed(authentication.getName(), page, size));
    }
}
