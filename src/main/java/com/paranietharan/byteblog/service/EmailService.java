package com.paranietharan.byteblog.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    public void sendVerificationEmail(String email, String name, String verificationToken) {
        // TODO: Integrate with actual email service (Gmail, SendGrid, etc.)
        log.info("Sending verification email to: {} ({})", name, email);
        log.debug("Verification token: {}", verificationToken);
        log.info("Email verification link: http://localhost:8080/auth/verify-email?token={}", verificationToken);
    }

    public void sendEmailChangeVerification(String newEmail, String name, String verificationToken) {
        // TODO: Integrate with actual email service
        log.info("Sending email change verification to: {} ({})", name, newEmail);
        log.debug("Email change token: {}", verificationToken);
        log.info("Email change link: http://localhost:8080/users/email/verify-change?token={}", verificationToken);
    }

    public void sendPasswordChangeNotification(String email, String name) {
        // TODO: Integrate with actual email service
        log.info("Sending password change notification to: {} ({})", name, email);
    }
}
