package com.example.socialmedia.controller;

import com.example.socialmedia.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<List<NotificationService.NotificationDTO>> getNotifications(Authentication authentication) {
        return ResponseEntity.ok(notificationService.getUserNotifications(authentication.getName()));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication authentication) {
        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount(authentication.getName())));
    }

    @PostMapping("/mark-read")
    public ResponseEntity<Map<String, Object>> markAllAsRead(Authentication authentication) {
        int count = notificationService.markAllAsRead(authentication.getName());
        return ResponseEntity.ok(Map.of("marked", count));
    }
}
