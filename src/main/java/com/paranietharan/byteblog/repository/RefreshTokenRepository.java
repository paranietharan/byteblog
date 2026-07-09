package com.paranietharan.byteblog.repository;

import com.paranietharan.byteblog.entity.RefreshToken;
import com.paranietharan.byteblog.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findByUser(User user);

    List<RefreshToken> findByUserAndRevokedFalse(User user);

    @Query("DELETE FROM RefreshToken r WHERE r.expiryDate < ?1")
    void deleteExpiredTokens(LocalDateTime now);

    void deleteByUser(User user);

    void revokeAllUserTokens(User user);
}
