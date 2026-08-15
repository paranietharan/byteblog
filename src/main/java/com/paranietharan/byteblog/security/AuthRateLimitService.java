package com.paranietharan.byteblog.security;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class AuthRateLimitService {

    private final JdbcTemplate jdbcTemplate;

    public RateLimitDecision consume(String scope, String clientIdentity, int limit, int windowSeconds) {
        long epochSeconds = LocalDateTime.now(ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC);
        long windowEpoch = epochSeconds - Math.floorMod(epochSeconds, windowSeconds);
        LocalDateTime windowStart = LocalDateTime.ofEpochSecond(windowEpoch, 0, ZoneOffset.UTC);

        Integer count = jdbcTemplate.queryForObject("""
                INSERT INTO auth_rate_limits (scope, client_key, window_start, request_count)
                VALUES (?, ?, ?, 1)
                ON CONFLICT (scope, client_key, window_start)
                DO UPDATE SET request_count = auth_rate_limits.request_count + 1
                RETURNING request_count
                """, Integer.class, scope, hash(clientIdentity), windowStart);

        long retryAfter = Math.max(1, windowEpoch + windowSeconds - epochSeconds);
        return new RateLimitDecision(count != null && count <= limit, retryAfter);
    }

    @Scheduled(cron = "${app.security.rate-limit.cleanup-cron:0 15 3 * * *}")
    public void deleteExpiredWindows() {
        jdbcTemplate.update(
                "DELETE FROM auth_rate_limits WHERE window_start < ?",
                LocalDateTime.now(ZoneOffset.UTC).minusDays(2)
        );
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    public record RateLimitDecision(boolean allowed, long retryAfterSeconds) {
    }
}
