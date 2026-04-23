package com.example.socialmedia.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender javaMailSender;

    public EmailService(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    // The actual Gmail SMTP account (used for transport auth)
    @Value("${spring.mail.username}")
    private String smtpUsername;

    // Display name shown to recipients — "FriendsHub"
    @Value("${app.mail.display-name:FriendsHub}")
    private String displayName;

    // From address shown to recipients — noreply@friendshub.me
    // Gmail SMTP allows custom From addresses; if blocked, falls back to smtpUsername
    @Value("${app.mail.from-address:noreply@friendshub.me}")
    private String fromAddress;

    // Reply-To address
    @Value("${app.mail.reply-to:support@friendshub.me}")
    private String replyTo;

    // Configurable verification base URL — production: https://friendshub.me/verify
    @Value("${app.verification-url:https://friendshub.me/verify}")
    private String verificationUrl;

    // Configurable password-reset base URL — production: https://friendshub.me/reset-password
    @Value("${app.reset-password-url:https://friendshub.me/reset-password}")
    private String resetPasswordUrl;

    // -------------------------------------------------------------------------
    // Verification Email
    // -------------------------------------------------------------------------

    @Async
    public void sendVerificationEmail(String toEmail, String token, String firstName) {
        log.info("Preparing verification email for {}", toEmail);
        try {
            String verifyLink = verificationUrl + "?token=" + token;
            String subject   = "Verify your FriendsHub account, " + firstName + "!";
            String htmlBody  = buildVerificationHtml(firstName, verifyLink);

            sendHtmlEmail(toEmail, subject, htmlBody);
            log.info("Verification email successfully sent to {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", toEmail, e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Password Reset Email
    // -------------------------------------------------------------------------

    @Async
    public void sendPasswordResetEmail(String toEmail, String token) {
        log.info("Preparing password reset email for {}", toEmail);
        try {
            String resetLink = resetPasswordUrl + "?token=" + token;
            String subject   = "Reset your FriendsHub password";
            String htmlBody  = buildPasswordResetHtml(resetLink);

            sendHtmlEmail(toEmail, subject, htmlBody);
            log.info("Password reset email successfully sent to {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send reset email to {}: {}", toEmail, e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Core send method
    // -------------------------------------------------------------------------

    private void sendHtmlEmail(String toEmail, String subject, String htmlBody)
            throws MessagingException, UnsupportedEncodingException {

        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        // "FriendsHub" <noreply@friendshub.me>
        // Gmail SMTP sends on behalf of this display name/address.
        // If your Gmail account blocks spoofed From, switch fromAddress to smtpUsername.
        helper.setFrom(new InternetAddress(fromAddress, displayName));
        helper.setReplyTo(new InternetAddress(replyTo, displayName));
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(htmlBody, true); // true = HTML

        javaMailSender.send(message);
    }

    // =========================================================================
    // HTML Templates (inline CSS — email-client safe, no external stylesheets)
    // =========================================================================

    private String buildVerificationHtml(String firstName, String verifyLink) {
        return "<!DOCTYPE html>" +
               "<html lang=\"en\">" +
               "<head>" +
               "  <meta charset=\"UTF-8\">" +
               "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
               "  <title>Verify your FriendsHub account</title>" +
               "</head>" +
               "<body style=\"margin:0;padding:0;background-color:#0a0a0a;font-family:'Helvetica Neue',Helvetica,Arial,sans-serif;\">" +

               // Outer wrapper
               "  <table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\"" +
               "         style=\"background-color:#0a0a0a;\">" +
               "    <tr><td align=\"center\" style=\"padding:40px 16px;\">" +

               // Card
               "      <table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\"" +
               "             style=\"max-width:580px;background-color:#111111;border-radius:16px;" +
               "                    border:1px solid #222222;overflow:hidden;\">" +

               // ── Header ──
               "        <tr>" +
               "          <td align=\"center\" style=\"padding:40px 48px 32px;background-color:#0f0f0f;" +
               "                                    border-bottom:1px solid #1e1e1e;\">" +
               "            <div style=\"display:inline-block;\">" +
               // Logo mark + wordmark
               "              <span style=\"display:inline-block;width:44px;height:44px;border-radius:12px;" +
               "                          background:linear-gradient(135deg,#6366f1 0%,#8b5cf6 100%);" +
               "                          text-align:center;line-height:44px;font-size:22px;vertical-align:middle;" +
               "                          margin-right:10px;\">&#10024;</span>" +
               "              <span style=\"font-size:26px;font-weight:800;color:#ffffff;vertical-align:middle;" +
               "                          letter-spacing:-0.5px;\">FriendsHub</span>" +
               "            </div>" +
               "          </td>" +
               "        </tr>" +

               // ── Body ──
               "        <tr>" +
               "          <td style=\"padding:40px 48px 32px;\">" +

               // Heading
               "            <h1 style=\"margin:0 0 16px;font-size:22px;font-weight:700;color:#ffffff;" +
               "                       line-height:1.3;\">Verify your email address</h1>" +

               // Body text
               "            <p style=\"margin:0 0 8px;font-size:15px;color:#a1a1aa;line-height:1.7;\">" +
               "              Hey " + escapeHtml(firstName) + "! \uD83D\uDC4B Welcome to FriendsHub." +
               "            </p>" +
               "            <p style=\"margin:0 0 32px;font-size:15px;color:#a1a1aa;line-height:1.7;\">" +
               "              Thanks for signing up. Click the button below to verify your email address" +
               "              and activate your account. This link expires in <strong style=\"color:#e4e4e7;\">24 hours</strong>." +
               "            </p>" +

               // CTA button
               "            <table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\">" +
               "              <tr>" +
               "                <td style=\"border-radius:10px;background:linear-gradient(135deg,#6366f1 0%,#8b5cf6 100%);\">" +
               "                  <a href=\"" + verifyLink + "\"" +
               "                     style=\"display:inline-block;padding:14px 32px;font-size:15px;font-weight:700;" +
               "                            color:#ffffff;text-decoration:none;border-radius:10px;letter-spacing:0.2px;\">" +
               "                    Verify My Account &rarr;" +
               "                  </a>" +
               "                </td>" +
               "              </tr>" +
               "            </table>" +

               // Fallback link
               "            <p style=\"margin:24px 0 0;font-size:12px;color:#52525b;line-height:1.6;\">" +
               "              Button not working? Copy and paste this link into your browser:<br>" +
               "              <a href=\"" + verifyLink + "\" style=\"color:#6366f1;word-break:break-all;\">" +
               verifyLink +
               "              </a>" +
               "            </p>" +
               "          </td>" +
               "        </tr>" +

               // ── Footer ──
               "        <tr>" +
               "          <td style=\"padding:24px 48px;background-color:#0f0f0f;border-top:1px solid #1e1e1e;\">" +
               "            <p style=\"margin:0 0 8px;font-size:12px;color:#52525b;line-height:1.6;\">" +
               "              If you didn&rsquo;t create a FriendsHub account, you can safely ignore this email." +
               "            </p>" +
               "            <p style=\"margin:0;font-size:12px;color:#3f3f46;\">" +
               "              &copy; 2025 FriendsHub &middot;" +
               "              <a href=\"https://friendshub.me\" style=\"color:#3f3f46;text-decoration:none;\">friendshub.me</a>" +
               "            </p>" +
               "          </td>" +
               "        </tr>" +

               "      </table>" + // end card

               "    </td></tr>" +
               "  </table>" + // end outer wrapper

               "</body></html>";
    }

    private String buildPasswordResetHtml(String resetLink) {
        return "<!DOCTYPE html>" +
               "<html lang=\"en\">" +
               "<head>" +
               "  <meta charset=\"UTF-8\">" +
               "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
               "  <title>Reset your FriendsHub password</title>" +
               "</head>" +
               "<body style=\"margin:0;padding:0;background-color:#0a0a0a;font-family:'Helvetica Neue',Helvetica,Arial,sans-serif;\">" +

               "  <table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\"" +
               "         style=\"background-color:#0a0a0a;\">" +
               "    <tr><td align=\"center\" style=\"padding:40px 16px;\">" +

               "      <table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\"" +
               "             style=\"max-width:580px;background-color:#111111;border-radius:16px;" +
               "                    border:1px solid #222222;overflow:hidden;\">" +

               // ── Header ──
               "        <tr>" +
               "          <td align=\"center\" style=\"padding:40px 48px 32px;background-color:#0f0f0f;" +
               "                                    border-bottom:1px solid #1e1e1e;\">" +
               "            <div style=\"display:inline-block;\">" +
               "              <span style=\"display:inline-block;width:44px;height:44px;border-radius:12px;" +
               "                          background:linear-gradient(135deg,#6366f1 0%,#8b5cf6 100%);" +
               "                          text-align:center;line-height:44px;font-size:22px;vertical-align:middle;" +
               "                          margin-right:10px;\">&#10024;</span>" +
               "              <span style=\"font-size:26px;font-weight:800;color:#ffffff;vertical-align:middle;" +
               "                          letter-spacing:-0.5px;\">FriendsHub</span>" +
               "            </div>" +
               "          </td>" +
               "        </tr>" +

               // ── Body ──
               "        <tr>" +
               "          <td style=\"padding:40px 48px 32px;\">" +

               "            <h1 style=\"margin:0 0 16px;font-size:22px;font-weight:700;color:#ffffff;" +
               "                       line-height:1.3;\">Reset your password</h1>" +

               "            <p style=\"margin:0 0 32px;font-size:15px;color:#a1a1aa;line-height:1.7;\">" +
               "              We received a request to reset your FriendsHub password. Click the button below" +
               "              to choose a new password. This link expires in <strong style=\"color:#e4e4e7;\">15 minutes</strong>." +
               "            </p>" +

               // CTA button — amber/orange to visually distinguish from verification
               "            <table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\">" +
               "              <tr>" +
               "                <td style=\"border-radius:10px;background:linear-gradient(135deg,#f59e0b 0%,#ef4444 100%);\">" +
               "                  <a href=\"" + resetLink + "\"" +
               "                     style=\"display:inline-block;padding:14px 32px;font-size:15px;font-weight:700;" +
               "                            color:#ffffff;text-decoration:none;border-radius:10px;letter-spacing:0.2px;\">" +
               "                    Reset My Password &rarr;" +
               "                  </a>" +
               "                </td>" +
               "              </tr>" +
               "            </table>" +

               "            <p style=\"margin:24px 0 0;font-size:12px;color:#52525b;line-height:1.6;\">" +
               "              Button not working? Copy and paste this link into your browser:<br>" +
               "              <a href=\"" + resetLink + "\" style=\"color:#6366f1;word-break:break-all;\">" +
               resetLink +
               "              </a>" +
               "            </p>" +
               "          </td>" +
               "        </tr>" +

               // ── Footer ──
               "        <tr>" +
               "          <td style=\"padding:24px 48px;background-color:#0f0f0f;border-top:1px solid #1e1e1e;\">" +
               "            <p style=\"margin:0 0 8px;font-size:12px;color:#52525b;line-height:1.6;\">" +
               "              If you didn&rsquo;t request a password reset, you can safely ignore this email." +
               "              Your password won&rsquo;t change." +
               "            </p>" +
               "            <p style=\"margin:0;font-size:12px;color:#3f3f46;\">" +
               "              &copy; 2025 FriendsHub &middot;" +
               "              <a href=\"https://friendshub.me\" style=\"color:#3f3f46;text-decoration:none;\">friendshub.me</a>" +
               "            </p>" +
               "          </td>" +
               "        </tr>" +

               "      </table>" +

               "    </td></tr>" +
               "  </table>" +

               "</body></html>";
    }

    // Basic HTML entity escaping to prevent XSS in email content
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;");
    }
}
