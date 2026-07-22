package com.example.socialmedia.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
import java.util.Random;
import com.example.socialmedia.entity.Role;
import com.example.socialmedia.entity.AuthProvider;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.Duration;
import com.example.socialmedia.dto.OAuthRequest;

@Service
public class AuthService {

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

        emailService.sendVerificationEmail(savedUser.getEmail(), rawToken, request.getFirstName());

        externalApiService.notifyUserRegistered(savedUser);

        return "User registered successfully. Please check your email to verify your account.";
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";

        String attemptsKey = "login:attempts:" + email;
        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(attemptsKey, Duration.ofMinutes(1));
        }
        if (attempts != null && attempts > 10) {
            throw new RuntimeException("Too many login attempts. Please try again in 1 minute.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            email,
                            request.getPassword()));
            redisTemplate.delete(attemptsKey);
        } catch (Exception e) {
            throw e;
        }

        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (user.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new RuntimeException("Please verify email first");
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

        // Constant-time comparison (redundant but following security best practices)
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
            String otp = String.format("%06d", new Random().nextInt(999999));
            redisTemplate.opsForValue().set("otp:" + normalizedEmail, otp, Duration.ofMinutes(10));
            emailService.sendOtpEmail(normalizedEmail, otp);
        }
        return "If that email exists, an OTP has been sent.";
    }

    @Transactional
    public String resetPassword(String email, String otp, String newPassword) {
        String normalizedEmail = email != null ? email.trim().toLowerCase() : "";
        String storedOtp = (String) redisTemplate.opsForValue().get("otp:" + normalizedEmail);
        if (storedOtp == null || !storedOtp.equals(otp)) {
            throw new RuntimeException("Invalid or expired OTP");
        }
        
        User user = userRepository.findByEmail(normalizedEmail).orElseThrow(() -> new RuntimeException("User not found"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        redisTemplate.delete("otp:" + normalizedEmail);
        return "Password reset successfully. You can now login.";
    }

    @Transactional
    public AuthResponse googleLogin(OAuthRequest request) {
        Optional<User> existingUser = userRepository.findByEmail(request.getEmail());
        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
        } else {
            user = new User();
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setVerificationStatus(com.example.socialmedia.entity.VerificationStatus.VERIFIED);
            user.setRole(Role.ROLE_USER);
            user.setAuthProvider(AuthProvider.GOOGLE);
            
            // Slugify name
            String slugified = request.getName().toLowerCase().replaceAll("[^a-z0-9]", "");
            user.setUsername(slugified);
            
            user = userRepository.save(user);

            UserInfo userInfo = new UserInfo();
            userInfo.setFirstName(request.getName());
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


