package com.example.socialmedia.controller;

import com.example.socialmedia.dto.AuthResponse;
import com.example.socialmedia.dto.MessageResponse;
import com.example.socialmedia.dto.LoginRequest;
import com.example.socialmedia.dto.RegisterRequest;
import com.example.socialmedia.security.JwtService;
import com.example.socialmedia.service.AuthService;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(new MessageResponse(authService.register(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verifyEmailGet(@RequestParam("token") String token) {
        return ResponseEntity.ok(new MessageResponse(authService.verifyAccount(token)));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyEmailPost(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        return ResponseEntity.ok(new MessageResponse(authService.verifyAccount(token)));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        return ResponseEntity.ok(new MessageResponse(authService.forgotPassword(email)));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String otp = body.get("otp");
        String newPassword = body.get("newPassword");
        return ResponseEntity.ok(new MessageResponse(authService.resetPassword(email, otp, newPassword)));
    }

    @PostMapping("/oauth/google")
    public ResponseEntity<AuthResponse> googleLogin(@RequestBody com.example.socialmedia.dto.OAuthRequest request) {
        return ResponseEntity.ok(authService.googleLogin(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(org.springframework.security.core.Authentication authentication) {
        return ResponseEntity.ok(authService.refreshToken(authentication.getName()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            jwtService.blacklistToken(token);
            return ResponseEntity.ok(new MessageResponse("Logged out successfully. Token has been revoked."));
        }
        return ResponseEntity.badRequest().body(new MessageResponse("Invalid authorization header"));
    }
}
