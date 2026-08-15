package com.paranietharan.byteblog.repository;

import com.paranietharan.byteblog.entity.RefreshToken;
import com.paranietharan.byteblog.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT token FROM RefreshToken token JOIN FETCH token.user WHERE token.tokenHash = :tokenHash")
    Optional<RefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Query("DELETE FROM RefreshToken r WHERE r.expiryDate < ?1")
    void deleteExpiredTokens(LocalDateTime now);

    @Modifying
    @Query("""
            UPDATE RefreshToken token
            SET token.revoked = true, token.revokedAt = :now
            WHERE token.familyId = :familyId AND token.revoked = false
            """)
    int revokeFamily(@Param("familyId") UUID familyId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("""
            UPDATE RefreshToken token
            SET token.revoked = true, token.revokedAt = :now
            WHERE token.user = :user AND token.revoked = false
            """)
    int revokeAllByUser(@Param("user") User user, @Param("now") LocalDateTime now);
}
