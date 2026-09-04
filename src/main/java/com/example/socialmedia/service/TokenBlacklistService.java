package com.example.socialmedia.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Date;

@Service
public class TokenBlacklistService {
    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    public TokenBlacklistService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void blacklistToken(String token, Date expiry) {
        long ttlMillis = expiry.getTime() - System.currentTimeMillis();
        if (ttlMillis > 0) {
            try {
                redisTemplate.opsForValue()
                        .set(getBlacklistKey(token), "1", Duration.ofMillis(ttlMillis));
                log.debug("Token blacklisted with TTL: {}ms", ttlMillis);
            } catch (Exception e) {
                // RESILIENCE: a Redis outage must not turn logout into a 500.
                // The token still expires naturally (15 min) even if not blacklisted.
                log.error("Failed to blacklist token on logout (Redis unavailable): {}", e.getMessage());
            }
        }
    }

    /**
     * PERFORMANCE/RESILIENCE: this is called on EVERY authenticated request.
     * If Redis is slow or down, failing hard would add seconds of latency to
     * every request (or break auth entirely). Fail open instead — the worst
     * case is that a logged-out token stays usable until its natural 15-minute
     * expiry, which is an acceptable trade-off versus a dead/slow API.
     */
    public boolean isBlacklisted(String token) {
        try {
            Boolean hasKey = redisTemplate.hasKey(getBlacklistKey(token));
            return Boolean.TRUE.equals(hasKey);
        } catch (Exception e) {
            log.warn("Blacklist check failed (Redis unavailable) — failing open: {}", e.getMessage());
            return false;
        }
    }

    public void removeTokenFromBlacklist(String token) {
        redisTemplate.delete(getBlacklistKey(token));
    }

    private String getBlacklistKey(String token) {
        if (token == null || token.isBlank()) return BLACKLIST_PREFIX + "empty";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return BLACKLIST_PREFIX + hex.toString();
        } catch (Exception e) {
            return BLACKLIST_PREFIX + token;
        }
    }
}
