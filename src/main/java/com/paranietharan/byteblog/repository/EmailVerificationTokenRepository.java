package com.paranietharan.byteblog.repository;

import com.paranietharan.byteblog.entity.EmailVerificationToken;
import com.paranietharan.byteblog.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findByToken(String token);

    Optional<EmailVerificationToken> findByUserAndUsedFalse(User user);

    @Query("DELETE FROM EmailVerificationToken e WHERE e.expiryDate < ?1")
    void deleteExpiredTokens(LocalDateTime now);

    long countByUserAndUsedFalse(User user);
}
