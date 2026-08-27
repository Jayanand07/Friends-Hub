package com.example.socialmedia.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Dedicated Gmail SMTP JavaMailSender Configuration.
 *
 * Automatically sanitizes credentials (stripping accidental whitespace/spaces
 * from Google App Passwords) and configures robust TLS, socket timeouts,
 * and SSL trust settings.
 */
@Configuration
public class MailConfig {

    private static final Logger log = LoggerFactory.getLogger(MailConfig.class);

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String host;

    @Value("${spring.mail.port:587}")
    private int port;

    @Value("${spring.mail.username:${MAIL_USERNAME:}}")
    private String username;

    @Value("${spring.mail.password:${MAIL_PASSWORD:}}")
    private String password;

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
            log.warn("Gmail SMTP username is not configured. Set MAIL_USERNAME environment variable.");
        }

        if (!cleanPassword.isBlank()) {
            mailSender.setPassword(cleanPassword);
        } else {
            log.warn("Gmail SMTP password is not configured. Set MAIL_PASSWORD environment variable.");
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

        return mailSender;
    }
}
