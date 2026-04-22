package com.example.socialmedia.service;

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

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserInfoRepository userInfoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final ExternalApiService externalApiService;

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
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already in use");
        }

        String verificationToken = UUID.randomUUID().toString();

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .verificationStatus(VerificationStatus.PENDING)
                .verificationToken(verificationToken)
                .tokenExpiry(LocalDateTime.now().plusHours(24))
                .build();

        User savedUser = userRepository.save(user);

        UserInfo userInfo = UserInfo.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .user(savedUser)
                .build();

        userInfoRepository.save(userInfo);

        emailService.sendVerificationEmail(savedUser.getEmail(), verificationToken);

        externalApiService.notifyUserRegistered(savedUser);

        return "User registered successfully. Please check your email to verify your account.";
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()));

        var user = userRepository.findByEmail(request.getEmail())
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
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid verification token"));

        if (user.getVerificationStatus() == VerificationStatus.VERIFIED) {
            return "Account already verified";
        }

        if (user.getTokenExpiry() != null && user.getTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Verification token has expired. Please register again.");
        }

        user.setVerificationStatus(VerificationStatus.VERIFIED);
        user.setVerificationToken(null);
        user.setTokenExpiry(null);
        userRepository.save(user);

        return "Account verified successfully";
    }

    @Transactional
    public String forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with this email"));

        String resetToken = UUID.randomUUID().toString();
        user.setPasswordResetToken(resetToken);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        emailService.sendPasswordResetEmail(email, resetToken);

        return "Password reset link has been sent to your email (Check terminal logs for token in dev mode)";
    }

    @Transactional
    public String resetPassword(String token, String newPassword) {
        User user = userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset token"));

        if (user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Reset token has expired");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);

        return "Password reset successfully. You can now login.";
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
}
