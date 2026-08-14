package com.paranietharan.byteblog.service;

import com.paranietharan.byteblog.config.MailNotificationProperties;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private MailNotificationProperties properties;
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        properties = new MailNotificationProperties();
        properties.setFrom("byteblog@gmail.com");
        properties.setFromName("Byteblog");
        properties.setApiBaseUrl("http://localhost:8080");
        properties.setFrontendBaseUrl("http://localhost:3000");
        emailService = new EmailService(mailSender, properties);
    }

    @Test
    void disabledNotificationsDoNotOpenMailConnection() {
        properties.setEnabled(false);

        emailService.sendVerificationEmail("user@example.com", "User", "token");

        verify(mailSender, never()).createMimeMessage();
    }

    @Test
    void enabledNotificationCreatesAndSendsMimeMessage() {
        properties.setEnabled(true);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);

        emailService.sendPasswordChangeNotification("user@example.com", "User");

        verify(mailSender).send(message);
    }
}
