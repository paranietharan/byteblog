package com.paranietharan.byteblog.service;

import com.paranietharan.byteblog.config.MailNotificationProperties;
import com.paranietharan.byteblog.entity.NotificationOutbox;
import com.paranietharan.byteblog.repository.NotificationOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private NotificationOutboxRepository outboxRepository;

    private MailNotificationProperties properties;
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        properties = new MailNotificationProperties();
        properties.setFrom("byteblog@gmail.com");
        properties.setFromName("Byteblog");
        properties.setApiBaseUrl("http://localhost:8080");
        properties.setFrontendBaseUrl("http://localhost:3000");
        emailService = new EmailService(outboxRepository, properties);
    }

    @Test
    void disabledNotificationsDoNotCreateOutboxRecord() {
        properties.setEnabled(false);

        emailService.sendVerificationEmail("user@example.com", "User", "token");

        verify(outboxRepository, never()).save(any());
    }

    @Test
    void enabledNotificationCreatesDurableHtmlOutboxRecord() {
        properties.setEnabled(true);

        emailService.sendVerificationEmail("user@example.com", "User", "token");

        ArgumentCaptor<NotificationOutbox> captor = ArgumentCaptor.forClass(NotificationOutbox.class);
        verify(outboxRepository).save(captor.capture());
        assertEquals("EMAIL_VERIFICATION", captor.getValue().getEventType());
        assertEquals("action", captor.getValue().getTemplateName());
        assertTrue(captor.getValue().getPayload().contains("api\\/v1\\/auth\\/verify-email")
                || captor.getValue().getPayload().contains("api/v1/auth/verify-email"));
    }
}
