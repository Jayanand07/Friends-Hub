package com.example.socialmedia.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Dedicated Gmail SMTP JavaMailSender Configuration.
 *
 * Automatically sanitizes credentials (stripping accidental whitespace/spaces
 * from Google App Passwords), checks password length, configures robust TLS,
 * socket timeouts, and verifies connectivity on startup.
 */
@Configuration
public class MailConfig {

    private static final Logger log = LoggerFactory.getLogger(MailConfig.class);

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String host;

    @Value("${MAIL_PORT:${spring.mail.port:587}}")
    private int port;

    @Value("${spring.mail.username:${MAIL_USERNAME:}}")
    private String username;

    @Value("${spring.mail.password:${MAIL_PASSWORD:}}")
    private String password;

    private JavaMailSenderImpl mailSenderInstance;

    @Bean
    @Primary
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host != null && !host.isBlank() ? host.trim() : "smtp.gmail.com");
        mailSender.setPort(port > 0 ? port : 587);
        mailSender.setDefaultEncoding("UTF-8");

        String cleanUsername = username != null ? username.trim() : "";
        String cleanPassword = password != null ? password.replaceAll("\\s+", "").trim() : "";

        if (!cleanUsername.isBlank()) {
            mailSender.setUsername(cleanUsername);
        } else {
            log.warn("[MailConfig] Gmail SMTP username is not configured. Set MAIL_USERNAME environment variable.");
        }

        if (!cleanPassword.isBlank()) {
            mailSender.setPassword(cleanPassword);
            // DIAGNOSIS: Google App Passwords are strictly 16 characters long.
            if (host != null && host.contains("gmail") && cleanPassword.length() != 16) {
                log.error("================================================================================");
                log.error("[MAIL CONFIG WARNING] Configured MAIL_PASSWORD is {} characters long!", cleanPassword.length());
                log.error("Google App Passwords MUST be exactly 16 characters (e.g. 'abcd efgh ijkl mnop').");
                log.error("If your password has 15 characters, a character was likely omitted when copying.");
                log.error("Please re-generate a 16-character App Password at https://myaccount.google.com/apppasswords");
                log.error("================================================================================");
            }
        } else {
            log.warn("[MailConfig] Gmail SMTP password is not configured. Set MAIL_PASSWORD environment variable.");
        }

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.trust", "*");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");

        if (port == 465) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.port", "465");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.fallback", "false");
        } else {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }

        log.info("Initialized JavaMailSender for Gmail SMTP (host: {}, port: {}, user: {})",
                mailSender.getHost(), mailSender.getPort(), cleanUsername.isEmpty() ? "UNCONFIGURED" : cleanUsername);

        this.mailSenderInstance = mailSender;
        return mailSender;
    }

    /**
     * Non-blocking connectivity test on startup to verify whether Gmail accepts
     * the configured credentials before a real user triggers forgot-password.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void verifyMailConnectionOnStartup() {
        if (mailSenderInstance == null) {
            return;
        }
        String cleanUser = mailSenderInstance.getUsername();
        String cleanPass = mailSenderInstance.getPassword();
        if (cleanUser == null || cleanUser.isBlank() || cleanPass == null || cleanPass.isBlank()) {
            log.warn("[MailConfig] Skipping SMTP startup check: MAIL_USERNAME or MAIL_PASSWORD not provided.");
            return;
        }

        // Test in a separate thread so application startup is never delayed or blocked
        Thread testThread = new Thread(() -> {
            try {
                log.info("[MailConfig] Performing startup connectivity check to Gmail SMTP ({}:{})...",
                        mailSenderInstance.getHost(), mailSenderInstance.getPort());
                mailSenderInstance.testConnection();
                log.info(">>> [MailConfig] GMAIL SMTP TEST SUCCEEDED! Emails and OTPs will be delivered. <<<");
            } catch (Exception e) {
                log.error("================================================================================");
                log.error(">>> [MailConfig] GMAIL SMTP TEST FAILED! <<<");
                log.error("Error: {}", e.getMessage());
                if (e.getMessage() != null && e.getMessage().contains("535")) {
                    log.error("DIAGNOSIS: Gmail rejected the username or App Password (535 Authentication Failed).");
                    log.error("1. Ensure 2-Step Verification is enabled on your Google account.");
                    log.error("2. Go to https://myaccount.google.com/apppasswords");
                    log.error("3. Generate a new App Password (select 'Other', name it 'FriendsHub').");
                    log.error("4. Copy all 16 characters (remove any spaces) into MAIL_PASSWORD in Render.");
                } else {
                    log.error("DIAGNOSIS: Unable to establish SMTP connection to {}:{}.",
                            mailSenderInstance.getHost(), mailSenderInstance.getPort());
                    log.error("If port 587 is blocked by your hosting provider, try setting MAIL_PORT=465.");
                }
                log.error("================================================================================");
            }
        }, "Mail-Startup-Checker");
        testThread.setDaemon(true);
        testThread.start();
    }
}

