package com.paranietharan.byteblog.service;

import com.paranietharan.byteblog.entity.RefreshToken;
import com.paranietharan.byteblog.entity.User;
import com.paranietharan.byteblog.exception.UnauthorizedException;
import com.paranietharan.byteblog.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository repository;

    private RefreshTokenService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenService(repository);
        ReflectionTestUtils.setField(service, "refreshTokenExpirationMs", 604800000L);
        user = new User();
        user.setId(UUID.randomUUID());
        user.setActive(true);
        user.setEmailVerified(true);
    }

    @Test
    void rotationRevokesPreviousTokenAndStoresOnlyReplacementHash() {
        String rawToken = "old-refresh-token";
        RefreshToken current = token(rawToken, false);
        when(repository.findByTokenHashForUpdate(RefreshTokenService.hash(rawToken)))
                .thenReturn(Optional.of(current));
        when(repository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshTokenService.RotationResult result = service.rotate(rawToken);

        assertTrue(current.getRevoked());
        assertNotEquals(rawToken, result.refreshToken());
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository, atLeastOnce()).save(captor.capture());
        RefreshToken replacement = captor.getAllValues().getLast();
        assertNotEquals(result.refreshToken(), replacement.getTokenHash());
        assertTrue(replacement.getTokenHash().matches("[0-9a-f]{64}"));
    }

    @Test
    void reuseOfRevokedTokenRevokesEntireFamily() {
        String rawToken = "replayed-refresh-token";
        RefreshToken replayed = token(rawToken, true);
        when(repository.findByTokenHashForUpdate(RefreshTokenService.hash(rawToken)))
                .thenReturn(Optional.of(replayed));

        assertThrows(UnauthorizedException.class, () -> service.rotate(rawToken));

        assertTrue(replayed.getReuseDetected());
        verify(repository).revokeFamily(any(UUID.class), any(LocalDateTime.class));
    }

    @Test
    void refreshRejectsUnverifiedAccountAndRevokesFamily() {
        user.setEmailVerified(false);
        String rawToken = "unverified-refresh-token";
        RefreshToken current = token(rawToken, false);
        when(repository.findByTokenHashForUpdate(RefreshTokenService.hash(rawToken)))
                .thenReturn(Optional.of(current));

        assertThrows(UnauthorizedException.class, () -> service.rotate(rawToken));

        verify(repository).revokeFamily(any(UUID.class), any(LocalDateTime.class));
    }

    private RefreshToken token(String rawToken, boolean revoked) {
        RefreshToken token = new RefreshToken();
        token.setId(UUID.randomUUID());
        token.setTokenHash(RefreshTokenService.hash(rawToken));
        token.setFamilyId(UUID.randomUUID());
        token.setUser(user);
        token.setExpiryDate(LocalDateTime.now().plusHours(1));
        token.setRevoked(revoked);
        token.setReuseDetected(false);
        return token;
    }
}
