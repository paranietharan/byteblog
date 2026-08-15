package com.paranietharan.byteblog.service;

import com.paranietharan.byteblog.config.MailNotificationProperties;
import com.paranietharan.byteblog.entity.NotificationOutbox;
import com.paranietharan.byteblog.entity.OutboxStatus;
import com.paranietharan.byteblog.repository.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.StringWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final NotificationOutboxRepository outboxRepository;
    private final MailNotificationProperties properties;

    @Transactional
    public void sendVerificationEmail(String email, String name, String verificationToken) {
        enqueue("EMAIL_VERIFICATION", email, "Verify your Byteblog email", name, "Verify your email",
                "Confirm your email address to activate your Byteblog account.", "Verify email",
                apiUrl("/api/v1/auth/verify-email?token=" + encode(verificationToken)));
    }

    @Transactional
    public void sendWelcomeEmail(String email, String name) {
        enqueue("WELCOME", email, "Welcome to Byteblog", name, "Your account is ready",
                "Your email address has been verified. You can now publish, comment, and like posts.",
                "Open Byteblog", frontendUrl(""));
    }

    @Transactional
    public void sendLoginNotification(String email, String name) {
        enqueue("LOGIN", email, "New sign-in to your Byteblog account", name, "New sign-in detected",
                "Your Byteblog account was signed in successfully. If this was not you, change your password immediately.",
                "Open your account", frontendUrl("/account"));
    }

    @Transactional
    public void sendPasswordChangeNotification(String email, String name) {
        enqueue("PASSWORD_CHANGED", email, "Your Byteblog password was changed", name, "Password changed",
                "Your password was changed and all refresh tokens were revoked. If this was not you, secure your account immediately.",
                "Open your account", frontendUrl("/account"));
    }

    @Transactional
    public void sendNameChangeNotification(String email, String name) {
        enqueue("PROFILE_CHANGED", email, "Your Byteblog profile was updated", name, "Profile name updated",
                "Your Byteblog display name was changed successfully.", "Open your account", frontendUrl("/account"));
    }

    @Transactional
    public void sendEmailChangeVerification(String newEmail, String name, String verificationToken) {
        enqueue("EMAIL_CHANGE_VERIFICATION", newEmail, "Confirm your new Byteblog email", name,
                "Confirm your new email", "Use this link to confirm that this address should become the email for your Byteblog account.",
                "Confirm email change", apiUrl("/api/v1/users/email/verify-change?token=" + encode(verificationToken)));
    }

    @Transactional
    public void sendEmailChangedNotification(String oldEmail, String newEmail, String name) {
        enqueue("EMAIL_CHANGED_OLD", oldEmail, "Your Byteblog email was changed", name, "Email address changed",
                "The email on your Byteblog account was changed to " + newEmail + ". If this was not you, secure your account immediately.",
                null, null);
        enqueue("EMAIL_CHANGED_NEW", newEmail, "Your new Byteblog email is active", name, "Email address confirmed",
                "This address is now connected to your Byteblog account. Sign in again using your new email.",
                "Sign in", frontendUrl("/login"));
    }

    @Transactional
    public void sendPostPublishedNotification(String email, String name, String title, String slug) {
        enqueue("POST_PUBLISHED", email, "Your Byteblog post is published", name, "Post published",
                "Your post “" + title + "” is now publicly available.", "View post", postUrl(slug));
    }

    @Transactional
    public void sendNewCommentNotification(
            String email, String name, String commenterName, String postTitle, String slug, String comment) {
        enqueue("NEW_COMMENT", email, "New comment on your Byteblog post", name, "New comment",
                commenterName + " commented on “" + postTitle + "”: “" + abbreviate(comment, 240) + "”",
                "View post", postUrl(slug));
    }

    @Transactional
    public void sendNewLikeNotification(String email, String name, String likerName, String postTitle, String slug) {
        enqueue("NEW_LIKE", email, "Someone liked your Byteblog post", name, "New like",
                likerName + " liked your post “" + postTitle + "”.", "View post", postUrl(slug));
    }

    @Transactional
    public void sendPostModerationNotification(
            String email, String name, String title, String slug, String action) {
        boolean deleted = "deleted".equals(action);
        enqueue("POST_MODERATED", email, "Your Byteblog post was " + action, name, "Post " + action,
                "An administrator " + action + " your post “" + title + "”.",
                deleted ? null : "View post", deleted ? null : postUrl(slug));
    }

    @Transactional
    public void sendCommentModerationNotification(
            String email, String name, String postTitle, String slug, String action) {
        boolean deleted = "deleted".equals(action);
        enqueue("COMMENT_MODERATED", email, "Your Byteblog comment was " + action, name, "Comment " + action,
                "An administrator " + action + " your comment on “" + postTitle + "”.",
                deleted ? null : "View post", deleted ? null : postUrl(slug));
    }

    private void enqueue(
            String eventType,
            String recipient,
            String subject,
            String name,
            String heading,
            String message,
            String buttonText,
            String buttonUrl) {
        if (!properties.isEnabled()) {
            log.debug("Email delivery disabled; skipping {} notification", eventType);
            return;
        }
        if (!StringUtils.hasText(recipient) || !StringUtils.hasText(properties.getFrom())) {
            log.error("Email notification skipped because recipient or sender is missing for event {}", eventType);
            return;
        }

        NotificationOutbox outbox = new NotificationOutbox();
        outbox.setEventType(eventType);
        outbox.setRecipient(recipient);
        outbox.setSubject(subject);
        outbox.setTemplateName(StringUtils.hasText(buttonUrl) ? "action" : "notice");
        outbox.setPayload(serializePayload(name, heading, message, buttonText, buttonUrl));
        outbox.setStatus(OutboxStatus.PENDING);
        outbox.setAttempts(0);
        outbox.setMaxAttempts(properties.getOutboxMaxAttempts());
        outbox.setAvailableAt(LocalDateTime.now());
        outbox.setIdempotencyKey(eventType + ":" + UUID.randomUUID());
        outboxRepository.save(outbox);
    }

    private String serializePayload(String name, String heading, String message, String buttonText, String buttonUrl) {
        Properties payload = new Properties();
        payload.setProperty("name", value(name));
        payload.setProperty("heading", value(heading));
        payload.setProperty("message", value(message));
        payload.setProperty("buttonText", value(buttonText));
        payload.setProperty("buttonUrl", value(buttonUrl));
        try {
            StringWriter writer = new StringWriter();
            payload.store(writer, null);
            return writer.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize notification payload", exception);
        }
    }

    private String value(String value) {
        return value == null ? "" : value;
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
}
