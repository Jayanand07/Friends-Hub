package com.example.socialmedia.controller;

import com.example.socialmedia.dto.*;
import com.example.socialmedia.security.JwtService;
import com.example.socialmedia.service.AuthService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Validated
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
    public ResponseEntity<?> verifyEmailGet(@RequestParam("token") @NotBlank(message = "Token is required") String token) {
        return ResponseEntity.ok(new MessageResponse(authService.verifyAccount(token)));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyEmailPost(@Valid @RequestBody VerifyRequest request) {
        return ResponseEntity.ok(new MessageResponse(authService.verifyAccount(request.getToken())));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(new MessageResponse(authService.resendVerification(request.getEmail())));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(new MessageResponse(authService.forgotPassword(request.getEmail())));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(new MessageResponse(authService.resetPassword(request.getEmail(), request.getOtp(), request.getNewPassword())));
    }

    @PostMapping("/oauth/google")
    public ResponseEntity<AuthResponse> googleLogin(@RequestBody OAuthRequest request) {
        return ResponseEntity.ok(authService.googleLogin(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) LogoutRequest body) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(new MessageResponse("Invalid authorization header"));
        }
        String token = authHeader.substring(7);
        jwtService.blacklistToken(token);

        // SECURITY (H-3): Also revoke the refresh token so the session cannot be
        // silently re-established after logout. The frontend must send it.
        if (body != null && body.getRefreshToken() != null && !body.getRefreshToken().isBlank()) {
            authService.revokeRefreshToken(body.getRefreshToken());
        }
        return ResponseEntity.ok(new MessageResponse("Logged out successfully. Tokens have been revoked."));
    }

    /** Optional logout payload carrying the refresh token to revoke. */
    public static class LogoutRequest {
        private String refreshToken;

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }
    }
}
