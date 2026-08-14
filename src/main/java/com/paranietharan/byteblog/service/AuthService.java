package com.paranietharan.byteblog.service;

import com.paranietharan.byteblog.dto.*;
import com.paranietharan.byteblog.entity.EmailVerificationToken;
import com.paranietharan.byteblog.entity.RefreshToken;
import com.paranietharan.byteblog.entity.User;
import com.paranietharan.byteblog.exception.BadRequestException;
import com.paranietharan.byteblog.exception.ResourceNotFoundException;
import com.paranietharan.byteblog.exception.UnauthorizedException;
import com.paranietharan.byteblog.repository.EmailVerificationTokenRepository;
import com.paranietharan.byteblog.repository.RefreshTokenRepository;
import com.paranietharan.byteblog.repository.UserRepository;
import com.paranietharan.byteblog.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository emailTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final EmailService emailService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            User existingUser = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new IllegalStateException("User should exist"));
            if (!existingUser.getEmailVerified()) {
                deleteAllEmailVerifcationTokenForUser(existingUser);

                String verificationToken = generateAndSaveEmailVerificationToken(existingUser);

                emailService.sendVerificationEmail(existingUser.getEmail(), existingUser.getName(), verificationToken);
                log.info("Verification email sent to: {}", existingUser.getEmail());

                String accessToken = tokenProvider.generateAccessTokenFromEmail(existingUser.getEmail());
                String refreshToken = generateAndSaveRefreshToken(existingUser);

                return buildAuthResponse(existingUser, accessToken, refreshToken);
            }

            throw new BadRequestException("Email already registered");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmailVerified(false);
        user.setActive(true);

        User savedUser = userRepository.save(user);
        log.info("New user registered: {}", savedUser.getEmail());

        String verificationToken = generateAndSaveEmailVerificationToken(savedUser);

        emailService.sendVerificationEmail(savedUser.getEmail(), savedUser.getName(), verificationToken);
        log.info("Verification email sent to: {}", savedUser.getEmail());

        String accessToken = tokenProvider.generateAccessTokenFromEmail(savedUser.getEmail());
        String refreshToken = generateAndSaveRefreshToken(savedUser);

        return buildAuthResponse(savedUser, accessToken, refreshToken);
    }

    public MessageResponse verifyEmail(String token) {
        EmailVerificationToken verificationToken = emailTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid verification token"));

        if (!verificationToken.canBeUsed()) {
            throw new BadRequestException("Verification token expired or already used");
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(LocalDateTime.now());
        userRepository.save(user);

        verificationToken.setUsed(true);
        verificationToken.setUsedAt(LocalDateTime.now());
        emailTokenRepository.save(verificationToken);

        deleteAllEmailVerifcationTokenForUser(user);

        log.info("Email verified for user: {}", user.getEmail());
        emailService.sendWelcomeEmail(user.getEmail(), user.getName());
        return new MessageResponse("Email verified successfully", true);
    }

    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            if (!user.getEmailVerified()) {
                throw new UnauthorizedException("Email not verified. Please verify your email first");
            }

            String accessToken = tokenProvider.generateAccessToken(authentication);
            String refreshToken = generateAndSaveRefreshToken(user);

            log.info("User logged in: {}", user.getEmail());
            emailService.sendLoginNotification(user.getEmail(), user.getName());
            return buildAuthResponse(user, accessToken, refreshToken);

        } catch (org.springframework.security.core.AuthenticationException e) {
            log.warn("Login failed for email: {}", request.getEmail());
            throw new UnauthorizedException("Invalid email or password");
        }
    }

    public AuthResponse refreshAccessToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));

        if (!refreshToken.isValid()) {
            throw new UnauthorizedException("Refresh token expired or revoked");
        }

        User user = refreshToken.getUser();
        String newAccessToken = tokenProvider.generateAccessTokenFromEmail(user.getEmail());

        log.info("Access token refreshed for user: {}", user.getEmail());
        return AuthResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .accessToken(newAccessToken)
                .refreshToken(request.getRefreshToken())
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getAccessTokenExpirationMs() / 1000)
                .emailVerified(user.getEmailVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public MessageResponse logout(String refreshToken) {
        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));

        token.setRevoked(true);
        token.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(token);

        log.info("User logged out");
        return new MessageResponse("Logged out successfully", true);
    }

    private String generateAndSaveRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(tokenProvider.generateRefreshToken());
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(LocalDateTime.now().plusSeconds(tokenProvider.getRefreshTokenExpirationMs() / 1000));
        refreshToken.setRevoked(false);

        refreshTokenRepository.save(refreshToken);
        return refreshToken.getToken();
    }

    private String generateAndSaveEmailVerificationToken(User user) {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(user);
        token.setExpiryDate(LocalDateTime.now().plusHours(24));
        token.setUsed(false);

        emailTokenRepository.save(token);
        return token.getToken();
    }

    private long deleteAllEmailVerifcationTokenForUser(User user) {
        return emailTokenRepository.deleteByUser(user);
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getAccessTokenExpirationMs() / 1000)
                .emailVerified(user.getEmailVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }
}
