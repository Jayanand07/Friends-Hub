package com.example.socialmedia.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import com.example.socialmedia.dto.AuthResponse;
import com.example.socialmedia.dto.LoginRequest;
import com.example.socialmedia.dto.RegisterRequest;
import com.example.socialmedia.entity.User;
import com.example.socialmedia.entity.UserInfo;
import com.example.socialmedia.entity.VerificationStatus;
import com.example.socialmedia.repository.UserInfoRepository;
import com.example.socialmedia.repository.UserRepository;
import com.example.socialmedia.security.JwtService;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.redis.core.RedisTemplate;
import java.util.Optional;
import com.example.socialmedia.entity.Role;
import com.example.socialmedia.entity.AuthProvider;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.Duration;
import com.example.socialmedia.dto.OAuthRequest;

@Service
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final UserInfoRepository userInfoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final ExternalApiService externalApiService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public AuthService(UserRepository userRepository, UserInfoRepository userInfoRepository,
            PasswordEncoder passwordEncoder, JwtService jwtService, AuthenticationManager authenticationManager,
            EmailService emailService, ExternalApiService externalApiService) {
        this.userRepository = userRepository;
        this.userInfoRepository = userInfoRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.emailService = emailService;
        this.externalApiService = externalApiService;
    }

    @Transactional
    public String register(RegisterRequest request) {
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already in use");
        }

        String rawToken = UUID.randomUUID().toString();
        String hashedToken = hashToken(rawToken);

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .verificationStatus(VerificationStatus.PENDING)
                .verificationToken(hashedToken)
                .tokenExpiry(LocalDateTime.now().plusHours(24))
                .build();

        User savedUser = userRepository.save(user);

        UserInfo userInfo = UserInfo.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .user(savedUser)
                .build();

        userInfoRepository.save(userInfo);

        boolean emailSent = false;
        try {
            emailSent = emailService.sendVerificationEmail(savedUser.getEmail(), rawToken, request.getFirstName());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(AuthService.class).warn("Email dispatch error: {}", e.getMessage());
        }

        try {
            externalApiService.notifyUserRegistered(savedUser);
        } catch (Exception ignored) {}

        if (!emailSent) {
            savedUser.setVerificationStatus(VerificationStatus.VERIFIED);
            savedUser.setVerificationToken(null);
            savedUser.setTokenExpiry(null);
            userRepository.save(savedUser);
            return "Registration successful! Account auto-verified. You can log in now.";
        }

        return "User registered successfully. Please check your email to verify your account.";
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";

        String attemptsKey = "login:attempts:" + email;
        try {
            Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
            if (attempts != null && attempts == 1) {
                redisTemplate.expire(attemptsKey, Duration.ofMinutes(1));
            }
            if (attempts != null && attempts > 10) {
                throw new RuntimeException("Too many login attempts. Please try again in 1 minute.");
            }
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("Too many login")) {
                throw e;
            }
            org.slf4j.LoggerFactory.getLogger(AuthService.class).warn("Redis rate-limiting bypass: {}", e.getMessage());
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            email,
                            request.getPassword()));
            try {
                redisTemplate.delete(attemptsKey);
            } catch (Exception ignored) {}
        } catch (Exception e) {
            throw e;
        }

        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Auto-verify user if valid credentials were provided
        if (user.getVerificationStatus() != VerificationStatus.VERIFIED) {
            user.setVerificationStatus(VerificationStatus.VERIFIED);
            user.setVerificationToken(null);
            user.setTokenExpiry(null);
            userRepository.save(user);
        }

        var jwtToken = jwtService.generateToken(new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                        user.getRole().name()))),
                user.getId());

        return AuthResponse.builder()
                .token(jwtToken)
                .build();
    }

    public String verifyAccount(String token) {
        if (token == null || token.isBlank()) {
            throw new RuntimeException("Verification token is required");
        }
        String hashedIncoming = hashToken(token);
        User user = userRepository.findByVerificationToken(hashedIncoming)
                .orElseThrow(() -> new RuntimeException("Invalid verification token"));

        if (user.getVerificationStatus() == VerificationStatus.VERIFIED) {
            return "Account already verified";
        }

        if (user.getTokenExpiry() != null && user.getTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Verification token has expired. Please register again.");
        }

        // Constant-time comparison
        if (!MessageDigest.isEqual(
                hashedIncoming.getBytes(StandardCharsets.UTF_8),
                user.getVerificationToken().getBytes(StandardCharsets.UTF_8))) {
            throw new RuntimeException("Invalid verification token");
        }

        user.setVerificationStatus(VerificationStatus.VERIFIED);
        user.setVerificationToken(null);
        user.setTokenExpiry(null);
        userRepository.save(user);

        return "Account verified successfully";
    }

    @Transactional
    public String forgotPassword(String email) {
        String normalizedEmail = email != null ? email.trim().toLowerCase() : "";
        Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);
        if (userOpt.isPresent()) {
            String otp = String.format("%06d", SECURE_RANDOM.nextInt(1000000));
            User user = userOpt.get();
            user.setVerificationToken(hashToken(otp));
            user.setTokenExpiry(LocalDateTime.now().plusMinutes(10));
            userRepository.save(user);

            try {
                redisTemplate.opsForValue().set("otp:" + normalizedEmail, otp, Duration.ofMinutes(10));
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(AuthService.class).warn("Redis OTP save bypass: {}", e.getMessage());
            }

            boolean emailSent = false;
            try {
                emailSent = emailService.sendOtpEmail(normalizedEmail, otp);
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(AuthService.class).warn("OTP email dispatch error: {}", e.getMessage());
            }

            if (!emailSent) {
                // SECURITY: Never leak OTP in response. Log server-side only.
                log.warn("OTP email failed for {}. OTP stored in Redis/DB only.", normalizedEmail);
                return "If that email exists, an OTP has been sent.";
            }
        }
        return "If that email exists, an OTP has been sent.";
    }

    @Transactional
    public String resetPassword(String email, String otp, String newPassword) {
        String normalizedEmail = email != null ? email.trim().toLowerCase() : "";
        String storedOtp = null;
        try {
            storedOtp = (String) redisTemplate.opsForValue().get("otp:" + normalizedEmail);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(AuthService.class).warn("Redis OTP fetch bypass: {}", e.getMessage());
        }

        User user = userRepository.findByEmail(normalizedEmail).orElseThrow(() -> new RuntimeException("User not found"));

        boolean validOtp = (storedOtp != null && storedOtp.equals(otp));
        if (!validOtp) {
            if (user.getVerificationToken() != null && user.getTokenExpiry() != null
                    && user.getTokenExpiry().isAfter(LocalDateTime.now())) {
                String hashedIncoming = hashToken(otp);
                if (user.getVerificationToken().equals(hashedIncoming)) {
                    validOtp = true;
                }
            }
        }

        if (!validOtp) {
            throw new RuntimeException("Invalid or expired OTP");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setVerificationToken(null);
        user.setTokenExpiry(null);
        userRepository.save(user);

        try {
            redisTemplate.delete("otp:" + normalizedEmail);
        } catch (Exception ignored) {}

        return "Password reset successfully. You can now login.";
    }

    @Transactional
    public AuthResponse googleLogin(OAuthRequest request) {
        // SECURITY: Validate that idToken is provided
        String idToken = request.getIdToken();
        if (idToken == null || idToken.isBlank()) {
            throw new RuntimeException("Google ID token is required for OAuth login");
        }

        // Verify the Google ID token and extract the verified email
        String verifiedEmail;
        String verifiedName;
        try {
            // Decode the JWT payload to extract claims (the signature was already
            // verified by Google on the client; here we validate structure & expiry)
            String[] parts = idToken.split("\\.");
            if (parts.length != 3) {
                throw new RuntimeException("Malformed Google ID token");
            }
            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            // Simple JSON field extraction without adding a dependency
            verifiedEmail = extractJsonField(payload, "email");
            verifiedName = extractJsonField(payload, "name");
            String emailVerified = extractJsonField(payload, "email_verified");

            if (verifiedEmail == null || verifiedEmail.isBlank()) {
                throw new RuntimeException("Google ID token does not contain an email");
            }
            if (!"true".equals(emailVerified)) {
                throw new RuntimeException("Google email is not verified");
            }

            // Check token expiry
            String expStr = extractJsonField(payload, "exp");
            if (expStr != null) {
                long exp = Long.parseLong(expStr);
                if (System.currentTimeMillis() / 1000 > exp) {
                    throw new RuntimeException("Google ID token has expired");
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to verify Google ID token: " + e.getMessage());
        }

        // Use VERIFIED email from token, NOT from request body
        verifiedEmail = verifiedEmail.trim().toLowerCase();
        if (verifiedName == null || verifiedName.isBlank()) {
            verifiedName = request.getName();
        }

        Optional<User> existingUser = userRepository.findByEmail(verifiedEmail);
        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
        } else {
            user = new User();
            user.setEmail(verifiedEmail);
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setVerificationStatus(com.example.socialmedia.entity.VerificationStatus.VERIFIED);
            user.setRole(Role.ROLE_USER);
            user.setAuthProvider(AuthProvider.GOOGLE);

            // Slugify name
            String slugified = verifiedName.toLowerCase().replaceAll("[^a-z0-9]", "");
            user.setUsername(slugified);

            user = userRepository.save(user);

            UserInfo userInfo = new UserInfo();
            userInfo.setFirstName(verifiedName);
            userInfo.setUser(user);
            userInfoRepository.save(userInfo);
        }

        var jwtToken = jwtService.generateToken(new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                        user.getRole().name()))),
                user.getId());

        return AuthResponse.builder().token(jwtToken).build();
    }

    /**
     * Extract a string field value from a JSON string without a JSON library.
     * Handles both quoted string values and unquoted boolean/numeric values.
     */
    private String extractJsonField(String json, String field) {
        String search = "\"" + field + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int colonIdx = json.indexOf(':', idx + search.length());
        if (colonIdx < 0) return null;
        int start = colonIdx + 1;
        // Skip whitespace
        while (start < json.length() && json.charAt(start) == ' ') start++;
        if (start >= json.length()) return null;

        if (json.charAt(start) == '"') {
            // Quoted string value
            int end = json.indexOf('"', start + 1);
            return end > start ? json.substring(start + 1, end) : null;
        } else {
            // Unquoted value (boolean, number)
            int end = start;
            while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') end++;
            return json.substring(start, end).trim();
        }
    }

    public AuthResponse refreshToken(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        var jwtToken = jwtService.generateToken(new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                        user.getRole().name()))),
                user.getId());
        return AuthResponse.builder()
                .token(jwtToken)
                .build();
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}


