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
import java.util.Locale;
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

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return convertToUserResponse(user);
    }

    public MessageResponse changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            log.warn("Incorrect current password for user: {}", user.getEmail());
            throw new UnauthorizedException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        refreshTokenRepository.deleteByUser(user);

        log.info("Password changed for user: {}", user.getEmail());
        emailService.sendPasswordChangeNotification(user.getEmail(), user.getName());

        return new MessageResponse("Password changed successfully", true);
    }

    public UserResponse changeName(UUID userId, ChangeNameRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setName(request.getName().trim());
        User updatedUser = userRepository.save(user);

        log.info("Name changed for user: {}", user.getEmail());
        emailService.sendNameChangeNotification(updatedUser.getEmail(), updatedUser.getName());
        return convertToUserResponse(updatedUser);
    }

    public MessageResponse requestEmailChange(UUID userId, ChangeEmailRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String newEmail = request.getNewEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(newEmail)) {
            throw new BadRequestException("Email already registered");
        }

        if (user.getEmail().equalsIgnoreCase(newEmail)) {
            throw new BadRequestException("New email must be different from current email");
        }

        EmailVerificationToken token = new EmailVerificationToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(user);
        token.setExpiryDate(LocalDateTime.now().plusHours(24));
        token.setUsed(false);
        token.setTokenType("EMAIL_CHANGE");
        token.setPendingEmail(newEmail);

        emailTokenRepository.save(token);

        emailService.sendEmailChangeVerification(newEmail, user.getName(), token.getToken());

        log.info("Email change requested for user: {}", user.getEmail());
        return new MessageResponse("Verification email sent to new email address", true);
    }

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
        String newEmail = emailToken.getPendingEmail();
        if (newEmail == null || newEmail.isBlank()) {
            throw new BadRequestException("Email change token does not contain a new email address");
        }
        if (userRepository.existsByEmail(newEmail) && !user.getEmail().equalsIgnoreCase(newEmail)) {
            throw new BadRequestException("Email already registered");
        }

        String oldEmail = user.getEmail();
        user.setEmail(newEmail);
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(LocalDateTime.now());
        userRepository.save(user);
        refreshTokenRepository.deleteByUser(user);

        emailToken.setUsed(true);
        emailToken.setUsedAt(LocalDateTime.now());
        emailTokenRepository.save(emailToken);

        emailService.sendEmailChangedNotification(oldEmail, newEmail, user.getName());
        log.info("Email change verified for user: {}", user.getId());
        return new MessageResponse("Email changed successfully", true);
    }

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
