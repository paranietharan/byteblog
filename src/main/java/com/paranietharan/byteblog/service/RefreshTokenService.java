package com.paranietharan.byteblog.service;

import com.paranietharan.byteblog.entity.RefreshToken;
import com.paranietharan.byteblog.entity.User;
import com.paranietharan.byteblog.exception.BadRequestException;
import com.paranietharan.byteblog.exception.UnauthorizedException;
import com.paranietharan.byteblog.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-expiration}")
    private long refreshTokenExpirationMs;

    @Transactional
    public String issue(User user) {
        return createToken(user, UUID.randomUUID(), null);
    }

    @Transactional(noRollbackFor = UnauthorizedException.class)
    public RotationResult rotate(String rawToken) {
        String tokenHash = hash(rawToken);
        RefreshToken current = refreshTokenRepository.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));
        LocalDateTime now = LocalDateTime.now();

        if (Boolean.TRUE.equals(current.getRevoked())) {
            current.setReuseDetected(true);
            refreshTokenRepository.revokeFamily(current.getFamilyId(), now);
            refreshTokenRepository.save(current);
            throw new UnauthorizedException("Refresh token reuse detected; token family revoked");
        }

        if (current.isExpired()) {
            current.setRevoked(true);
            current.setRevokedAt(now);
            refreshTokenRepository.save(current);
            throw new UnauthorizedException("Refresh token expired");
        }

        User user = current.getUser();
        if (!Boolean.TRUE.equals(user.getActive()) || !Boolean.TRUE.equals(user.getEmailVerified())) {
            refreshTokenRepository.revokeFamily(current.getFamilyId(), now);
            throw new UnauthorizedException("Active, verified account required");
        }

        current.setRevoked(true);
        current.setRevokedAt(now);
        current.setRotatedAt(now);
        refreshTokenRepository.save(current);

        String replacement = createToken(user, current.getFamilyId(), current);
        return new RotationResult(user, replacement);
    }

    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHashForUpdate(hash(rawToken)).ifPresent(token -> {
            if (!Boolean.TRUE.equals(token.getRevoked())) {
                token.setRevoked(true);
                token.setRevokedAt(LocalDateTime.now());
                refreshTokenRepository.save(token);
            }
        });
    }

    @Transactional
    public void revokeAll(User user) {
        refreshTokenRepository.revokeAllByUser(user, LocalDateTime.now());
    }

    private String createToken(User user, UUID familyId, RefreshToken parent) {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        RefreshToken token = new RefreshToken();
        token.setTokenHash(hash(rawToken));
        token.setFamilyId(familyId);
        token.setParentToken(parent);
        token.setUser(user);
        token.setExpiryDate(LocalDateTime.now().plusSeconds(refreshTokenExpirationMs / 1000));
        token.setRevoked(false);
        token.setReuseDetected(false);
        refreshTokenRepository.save(token);
        return rawToken;
    }

    static String hash(String token) {
        if (token == null || token.isBlank()) {
            throw new BadRequestException("Refresh token is required");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    public record RotationResult(User user, String refreshToken) {
    }
}
