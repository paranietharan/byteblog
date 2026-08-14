package com.paranietharan.byteblog.service;

import com.paranietharan.byteblog.config.MailNotificationProperties;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final MailNotificationProperties properties;

    @Async("notificationExecutor")
    public void sendVerificationEmail(String email, String name, String verificationToken) {
        String link = apiUrl("/auth/verify-email?token=" + encode(verificationToken));
        if (!properties.isEnabled()) {
            log.debug("Email notifications are disabled; skipping verification email to {}", email);
            return;
        }
        sendEmail(
                email,
                "Verify your Byteblog email",
                name,
                "Verify your email",
                "Confirm your email address to activate your Byteblog account.",
                "Verify email",
                link
        );
    }

    @Async("notificationExecutor")
    public void sendWelcomeEmail(String email, String name) {
        sendEmail(
                email,
                "Welcome to Byteblog",
                name,
                "Your account is ready",
                "Your email address has been verified. You can now publish, comment, and like posts.",
                "Open Byteblog",
                frontendUrl("")
        );
    }

    @Async("notificationExecutor")
    public void sendLoginNotification(String email, String name) {
        sendEmail(
                email,
                "New sign-in to your Byteblog account",
                name,
                "New sign-in detected",
                "Your Byteblog account was signed in successfully. If this was not you, change your password immediately.",
                "Open your account",
                frontendUrl("/account")
        );
    }

    @Async("notificationExecutor")
    public void sendPasswordChangeNotification(String email, String name) {
        sendEmail(
                email,
                "Your Byteblog password was changed",
                name,
                "Password changed",
                "Your password was changed and all refresh tokens were revoked. If this was not you, secure your account immediately.",
                "Open your account",
                frontendUrl("/account")
        );
    }

    @Async("notificationExecutor")
    public void sendNameChangeNotification(String email, String name) {
        sendEmail(
                email,
                "Your Byteblog profile was updated",
                name,
                "Profile name updated",
                "Your Byteblog display name was changed successfully.",
                "Open your account",
                frontendUrl("/account")
        );
    }

    @Async("notificationExecutor")
    public void sendEmailChangeVerification(String newEmail, String name, String verificationToken) {
        String link = apiUrl("/users/email/verify-change?token=" + encode(verificationToken));
        if (!properties.isEnabled()) {
            log.debug("Email notifications are disabled; skipping email-change verification to {}", newEmail);
            return;
        }
        sendEmail(
                newEmail,
                "Confirm your new Byteblog email",
                name,
                "Confirm your new email",
                "Use this link to confirm that this address should become the email for your Byteblog account.",
                "Confirm email change",
                link
        );
    }

    @Async("notificationExecutor")
    public void sendEmailChangedNotification(String oldEmail, String newEmail, String name) {
        sendEmail(
                oldEmail,
                "Your Byteblog email was changed",
                name,
                "Email address changed",
                "The email on your Byteblog account was changed to " + newEmail + ". If this was not you, secure your account immediately.",
                null,
                null
        );
        sendEmail(
                newEmail,
                "Your new Byteblog email is active",
                name,
                "Email address confirmed",
                "This address is now connected to your Byteblog account. Sign in again using your new email.",
                "Sign in",
                frontendUrl("/login")
        );
    }

    @Async("notificationExecutor")
    public void sendPostPublishedNotification(String email, String name, String title, String slug) {
        sendEmail(
                email,
                "Your Byteblog post is published",
                name,
                "Post published",
                "Your post “" + title + "” is now publicly available.",
                "View post",
                postUrl(slug)
        );
    }

    @Async("notificationExecutor")
    public void sendNewCommentNotification(
            String email,
            String name,
            String commenterName,
            String postTitle,
            String slug,
            String comment) {
        sendEmail(
                email,
                "New comment on your Byteblog post",
                name,
                "New comment",
                commenterName + " commented on “" + postTitle + "”: “" + abbreviate(comment, 240) + "”",
                "View post",
                postUrl(slug)
        );
    }

    @Async("notificationExecutor")
    public void sendNewLikeNotification(
            String email,
            String name,
            String likerName,
            String postTitle,
            String slug) {
        sendEmail(
                email,
                "Someone liked your Byteblog post",
                name,
                "New like",
                likerName + " liked your post “" + postTitle + "”.",
                "View post",
                postUrl(slug)
        );
    }

    @Async("notificationExecutor")
    public void sendPostModerationNotification(
            String email,
            String name,
            String title,
            String slug,
            String action) {
        boolean deleted = "deleted".equals(action);
        sendEmail(
                email,
                "Your Byteblog post was " + action,
                name,
                "Post " + action,
                "An administrator " + action + " your post “" + title + "”.",
                deleted ? null : "View post",
                deleted ? null : postUrl(slug)
        );
    }

    @Async("notificationExecutor")
    public void sendCommentModerationNotification(
            String email,
            String name,
            String postTitle,
            String slug,
            String action) {
        boolean deleted = "deleted".equals(action);
        sendEmail(
                email,
                "Your Byteblog comment was " + action,
                name,
                "Comment " + action,
                "An administrator " + action + " your comment on “" + postTitle + "”.",
                deleted ? null : "View post",
                deleted ? null : postUrl(slug)
        );
    }

    private void sendEmail(
            String recipient,
            String subject,
            String name,
            String heading,
            String message,
            String buttonText,
            String buttonUrl) {
        if (!properties.isEnabled()) {
            return;
        }
        if (!StringUtils.hasText(recipient) || !StringUtils.hasText(properties.getFrom())) {
            log.error("Email notification skipped because the recipient or sender address is missing");
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
            helper.setFrom(properties.getFrom(), properties.getFromName());
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(buildPlainText(name, heading, message, buttonUrl), buildHtml(name, heading, message, buttonText, buttonUrl));
            mailSender.send(mimeMessage);
            log.info("Email notification sent to {} with subject: {}", recipient, subject);
        } catch (Exception exception) {
            log.error("Could not send email notification to {} with subject: {}", recipient, subject, exception);
        }
    }

    private String buildPlainText(String name, String heading, String message, String buttonUrl) {
        String greeting = StringUtils.hasText(name) ? "Hello " + name + "," : "Hello,";
        String link = StringUtils.hasText(buttonUrl) ? "\n\n" + buttonUrl : "";
        return greeting + "\n\n" + heading + "\n\n" + message + link + "\n\nByteblog";
    }

    private String buildHtml(String name, String heading, String message, String buttonText, String buttonUrl) {
        String greeting = StringUtils.hasText(name) ? "Hello " + escapeHtml(name) + "," : "Hello,";
        String button = StringUtils.hasText(buttonText) && StringUtils.hasText(buttonUrl)
                ? "<p style=\"margin:28px 0\"><a href=\"" + escapeHtml(buttonUrl)
                + "\" style=\"background:#2563eb;color:#fff;padding:12px 20px;border-radius:6px;text-decoration:none;display:inline-block\">"
                + escapeHtml(buttonText) + "</a></p>"
                : "";

        return """
                <!doctype html>
                <html lang="en">
                <body style="margin:0;background:#f3f4f6;font-family:Arial,sans-serif;color:#111827">
                  <div style="max-width:600px;margin:32px auto;background:#fff;border-radius:10px;padding:32px">
                    <div style="font-size:22px;font-weight:700;color:#2563eb;margin-bottom:24px">Byteblog</div>
                    <p>%s</p>
                    <h1 style="font-size:24px;margin:20px 0 12px">%s</h1>
                    <p style="line-height:1.6;color:#374151">%s</p>
                    %s
                    <p style="margin-top:32px;color:#6b7280;font-size:13px">This is an automatic notification from Byteblog.</p>
                  </div>
                </body>
                </html>
                """.formatted(greeting, escapeHtml(heading), escapeHtml(message), button);
    }

    private String apiUrl(String path) {
        return normalizedBaseUrl(properties.getApiBaseUrl()) + path;
    }

    private String frontendUrl(String path) {
        return normalizedBaseUrl(properties.getFrontendBaseUrl()) + path;
    }

    private String postUrl(String slug) {
        return frontendUrl("/posts/" + encode(slug));
    }

    private String normalizedBaseUrl(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value == null ? "" : value;
        }
        return value.substring(0, maxLength - 1) + "…";
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
