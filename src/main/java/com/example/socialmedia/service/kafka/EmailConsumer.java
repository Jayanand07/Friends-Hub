package com.example.socialmedia.service.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.socialmedia.dto.kafka.EmailEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailConsumer {
    private static final Logger log =
        LoggerFactory.getLogger(EmailConsumer.class);

    private final JavaMailSender mailSender;
    public EmailConsumer(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // Simple in-memory cache for email templates
    private static final java.util.Map<String, String> TEMPLATE_SUBJECTS = new java.util.HashMap<>();
    private static final java.util.Map<String, String> TEMPLATE_BODIES = new java.util.HashMap<>();

    static {
        TEMPLATE_SUBJECTS.put("welcome", "Welcome to FriendsHub!");
        TEMPLATE_SUBJECTS.put("password-reset", "Reset your FriendsHub password");
        TEMPLATE_SUBJECTS.put("verify", "Verify your FriendsHub account");

        TEMPLATE_BODIES.put("welcome", "Welcome to FriendsHub, {name}!");
        TEMPLATE_BODIES.put("password-reset", "Your reset link: {link}");
        TEMPLATE_BODIES.put("verify", "Your verification link: {link}");
    }

    @KafkaListener(topics = "email-queue", groupId = "email-workers", concurrency = "2")
    public void handleEmail(EmailEvent event) {
        try {
            // Validate required fields to prevent NullPointerException crashes
            if (event == null) {
                log.error("Received null email event, skipping");
                return;
            }
            if (event.getTo() == null || event.getTo().trim().isEmpty()) {
                log.error("Email event missing 'to' address, skipping");
                return;
            }
            if (event.getSubject() == null || event.getSubject().trim().isEmpty()) {
                log.error("Email event missing subject, skipping for recipient: {}", maskEmail(event.getTo()));
                return;
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(event.getTo());
            message.setSubject(event.getSubject());

            // Build body from template or use custom subject/body
            String body = buildEmailBody(event);
            message.setText(body);

            mailSender.send(message);
            log.info("Email sent successfully to {}", maskEmail(event.getTo()));

        } catch (Exception e) {
            log.error("Email send failed: {}", e.getMessage());
            // Don't rethrow for email - you don't want infinite retries on bad addresses
        }
    }

    private String buildEmailBody(EmailEvent event) {
        String template = event.getTemplateName();
        java.util.Map<String, Object> vars = event.getVariables();

        // Use template if available, otherwise build from variables
        if (template != null && TEMPLATE_BODIES.containsKey(template)) {
            String bodyTemplate = TEMPLATE_BODIES.get(template);
            return substituteVariables(bodyTemplate, vars);
        }

        // Fallback: convert variables to string
        return vars != null ? vars.toString() : "Email content not available";
    }

    private String substituteVariables(String template, java.util.Map<String, Object> vars) {
        if (vars == null) return template;
        String result = template;
        for (java.util.Map.Entry<String, Object> entry : vars.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}",
                    entry.getValue() != null ? entry.getValue().toString() : "");
        }
        return result;
    }

    /** Mask email for logging: e.g. "user@example.com" -> "u*****@example.com" */
    private static String maskEmail(String email) {
        if (email == null || email.isBlank()) return email;
        int atIdx = email.indexOf('@');
        if (atIdx <= 1) return email;
        return email.charAt(0) + "*****" + email.substring(atIdx);
    }
}
