package com.example.socialmedia.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender javaMailSender;

    public EmailService(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Value("${spring.mail.username:noreply@socialmedia.com}")
    private String senderEmail;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Async
    public void sendVerificationEmail(String toEmail, String token) {
        log.info("Preparing verification email for {}", toEmail);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(toEmail);
            message.setSubject("Account Verification");
            message.setText("Click the link to verify your account: " +
                    frontendUrl + "/verify?token=" + token);
            javaMailSender.send(message);
            log.info("Verification email successfully sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", toEmail, e.getMessage());
            log.debug("Verification token for {}: {}", toEmail, token);
        }
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String token) {
        log.info("Preparing password reset email for {}", toEmail);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(toEmail);
            message.setSubject("Password Reset Request");
            message.setText("Click the link below to reset your password. This link will expire in 15 minutes.\n\n" +
                    frontendUrl + "/reset-password?token=" + token);
            javaMailSender.send(message);
            log.info("Password reset email successfully sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send reset email to {}: {}", toEmail, e.getMessage());
            log.debug("Reset token for {}: {}", toEmail, token);
        }
    }
}
