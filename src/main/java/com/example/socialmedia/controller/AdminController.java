package com.example.socialmedia.controller;

import com.example.socialmedia.dto.AdminActionLogResponse;
import com.example.socialmedia.dto.FollowUserResponse;
import com.example.socialmedia.service.AdminService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public ResponseEntity<Page<FollowUserResponse>> getAllUsers(Pageable pageable) {
        return ResponseEntity.ok(adminService.getAllUsers(pageable));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(Map.of("message", adminService.deleteUser(id, authentication.getName())));
    }

    @PostMapping("/block/{id}")
    public ResponseEntity<Map<String, String>> blockUser(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(Map.of("message", adminService.adminBlockUser(id, authentication.getName())));
    }

    @PostMapping("/unblock/{id}")
    public ResponseEntity<Map<String, String>> unblockUser(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(Map.of("message", adminService.adminUnblockUser(id, authentication.getName())));
    }

    @DeleteMapping("/posts/{id}")
    public ResponseEntity<Map<String, String>> deletePost(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(Map.of("message", adminService.deletePost(id, authentication.getName())));
    }

    @DeleteMapping("/comments/{id}")
    public ResponseEntity<Map<String, String>> deleteComment(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(Map.of("message", adminService.deleteComment(id, authentication.getName())));
    }

    @GetMapping("/logs")
    public ResponseEntity<List<AdminActionLogResponse>> getActionLogs() {
        return ResponseEntity.ok(adminService.getActionLogs());
    }
}
