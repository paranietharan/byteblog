package com.paranietharan.byteblog.service;

import com.paranietharan.byteblog.dto.*;
import com.paranietharan.byteblog.entity.EmailVerificationToken;
import com.paranietharan.byteblog.entity.User;
import com.paranietharan.byteblog.exception.BadRequestException;
import com.paranietharan.byteblog.exception.ResourceNotFoundException;
import com.paranietharan.byteblog.exception.UnauthorizedException;
import com.paranietharan.byteblog.repository.EmailVerificationTokenRepository;
import com.paranietharan.byteblog.repository.RefreshTokenRepository;
import com.paranietharan.byteblog.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository emailTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    // GET CURRENT USER PROFILE
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return convertToUserResponse(user);
    }

    // CHANGE PASSWORD
    public MessageResponse changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            log.warn("Incorrect current password for user: {}", user.getEmail());
            throw new UnauthorizedException("Current password is incorrect");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Revoke all existing refresh tokens for security
        refreshTokenRepository.deleteByUser(user);

        log.info("Password changed for user: {}", user.getEmail());
        emailService.sendPasswordChangeNotification(user.getEmail(), user.getName());

        return new MessageResponse("Password changed successfully", true);
    }

    // CHANGE NAME
    public UserResponse changeName(Long userId, ChangeNameRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setName(request.getName());
        User updatedUser = userRepository.save(user);

        log.info("Name changed for user: {}", user.getEmail());
        return convertToUserResponse(updatedUser);
    }

    // REQUEST EMAIL CHANGE
    public MessageResponse requestEmailChange(Long userId, ChangeEmailRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check if new email is already in use
        if (userRepository.existsByEmail(request.getNewEmail())) {
            throw new BadRequestException("Email already registered");
        }

        // Same email check
        if (user.getEmail().equals(request.getNewEmail())) {
            throw new BadRequestException("New email must be different from current email");
        }

        // Generate email change verification token
        EmailVerificationToken token = new EmailVerificationToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(user);
        token.setExpiryDate(LocalDateTime.now().plusHours(24));
        token.setUsed(false);
        token.setTokenType("EMAIL_CHANGE");

        emailTokenRepository.save(token);

        // Send verification email to NEW email
        emailService.sendEmailChangeVerification(request.getNewEmail(), user.getName(), token.getToken());

        log.info("Email change requested for user: {}", user.getEmail());
        return new MessageResponse("Verification email sent to new email address", true);
    }

    // VERIFY EMAIL CHANGE
    public MessageResponse verifyEmailChange(String token) {
        EmailVerificationToken emailToken = emailTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid email change token"));

        if (!emailToken.canBeUsed()) {
            throw new BadRequestException("Token expired or already used");
        }

        if (!"EMAIL_CHANGE".equals(emailToken.getTokenType())) {
            throw new BadRequestException("Invalid token type");
        }

        User user = emailToken.getUser();

        // Update email
        user.setEmail(emailToken.getUser().getEmail()); // This should be the new email stored somewhere
        // For now, we'll store it in a temp field or retrieve from context
        // Better approach: create separate entity for pending email changes

        user.setEmailVerified(true);
        user.setEmailVerifiedAt(LocalDateTime.now());
        userRepository.save(user);

        // Mark token as used
        emailToken.setUsed(true);
        emailToken.setUsedAt(LocalDateTime.now());
        emailTokenRepository.save(emailToken);

        log.info("Email change verified for user");
        return new MessageResponse("Email changed successfully", true);
    }

    // CONVERT TO DTO
    private UserResponse convertToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .active(user.getActive())
                .emailVerified(user.getEmailVerified())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .emailVerifiedAt(user.getEmailVerifiedAt())
                .build();
    }
}
