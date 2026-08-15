package com.paranietharan.byteblog.service;

import com.paranietharan.byteblog.config.MailNotificationProperties;
import com.paranietharan.byteblog.entity.NotificationOutbox;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationOutboxWorker {

    private final OutboxClaimService claimService;
    private final JavaMailSender mailSender;
    private final EmailTemplateRenderer templateRenderer;
    private final MailNotificationProperties properties;

    @Scheduled(fixedDelayString = "${app.mail.outbox-fixed-delay-ms:5000}")
    public void deliverPendingNotifications() {
        if (!properties.isEnabled()) {
            return;
        }
        for (int index = 0; index < properties.getOutboxBatchSize(); index++) {
            NotificationOutbox outbox = claimService.claimNext().orElse(null);
            if (outbox == null) {
                return;
            }
            try {
                deliver(outbox);
                claimService.markSent(outbox.getId());
            } catch (Exception exception) {
                log.warn("Notification outbox delivery failed for {} on attempt {}", outbox.getId(), outbox.getAttempts());
                claimService.markFailed(outbox.getId(), exception);
            }
        }
    }

    @Scheduled(fixedDelayString = "${app.mail.outbox-recovery-delay-ms:60000}")
    public void recoverStaleNotifications() {
        int recovered = claimService.recoverStaleClaims();
        if (recovered > 0) {
            log.warn("Recovered {} stale notification outbox claims", recovered);
        }
    }

    private void deliver(NotificationOutbox outbox) throws Exception {
        Map<String, String> payload = parsePayload(outbox.getPayload());
        String html = templateRenderer.render(outbox.getTemplateName(), payload);
        String plainText = "Hello " + payload.getOrDefault("name", "") + ",\n\n"
                + payload.getOrDefault("heading", "") + "\n\n"
                + payload.getOrDefault("message", "") + "\n\n"
                + payload.getOrDefault("buttonUrl", "") + "\n\nByteblog";

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
        helper.setFrom(properties.getFrom(), properties.getFromName());
        helper.setTo(outbox.getRecipient());
        helper.setSubject(outbox.getSubject());
        helper.setText(plainText, html);
        mailSender.send(message);
    }

    private Map<String, String> parsePayload(String serialized) throws Exception {
        Properties values = new Properties();
        values.load(new StringReader(serialized));
        Map<String, String> payload = new HashMap<>();
        values.forEach((key, value) -> payload.put(key.toString(), value.toString()));
        return payload;
    }
}
