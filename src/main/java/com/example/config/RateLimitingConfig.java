package com.example.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiting Configuration using Bucket4j
 * Implements Token Bucket algorithm for API rate limiting
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(RateLimitingProperties.class)
public class RateLimitingConfig {

    @Value("${app.ratelimit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${app.ratelimit.global.requests:1000}")
    private int globalRequests;

    @Value("${app.ratelimit.global.duration-seconds:3600}")
    private int globalDurationSeconds;

    @Value("${app.ratelimit.per-user.requests:100}")
    private int perUserRequests;

    @Value("${app.ratelimit.per-user.duration-seconds:3600}")
    private int perUserDurationSeconds;

    public RateLimitingConfig() {
        log.info("RateLimitingConfig initialized");
    }

    /**
     * Creates a global rate limiter bucket
     */
    public Bucket createGlobalBucket() {
        if (!rateLimitEnabled) {
            return createUnlimitedBucket();
        }
        Bandwidth limit = Bandwidth.classic(globalRequests, Refill.intervally(globalRequests, Duration.ofSeconds(globalDurationSeconds)));
        return Bucket4j.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Creates a per-user rate limiter bucket
     */
    public Bucket createPerUserBucket() {
        if (!rateLimitEnabled) {
            return createUnlimitedBucket();
        }
        Bandwidth limit = Bandwidth.classic(perUserRequests, Refill.intervally(perUserRequests, Duration.ofSeconds(perUserDurationSeconds)));
        return Bucket4j.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Creates a per-endpoint rate limiter bucket
     */
    public Bucket createEndpointBucket(int requests, int durationSeconds) {
        if (!rateLimitEnabled) {
            return createUnlimitedBucket();
        }
        Bandwidth limit = Bandwidth.classic(requests, Refill.intervally(requests, Duration.ofSeconds(durationSeconds)));
        return Bucket4j.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Creates an unlimited bucket for when rate limiting is disabled
     */
    private Bucket createUnlimitedBucket() {
        return Bucket4j.builder()
                .addLimit(Bandwidth.classic(Integer.MAX_VALUE, Refill.intervally(Integer.MAX_VALUE, Duration.ofSeconds(1))))
                .build();
    }

    /**
     * Returns whether rate limiting is enabled
     */
    public boolean isRateLimitEnabled() {
        return rateLimitEnabled;
    }
}
@Component
@Slf4j
class RateLimiterManager {

    private final Map<String, Bucket> userBuckets = new ConcurrentHashMap<>();
    private final RateLimitingConfig rateLimitingConfig;
    private final Bucket globalBucket;

    public RateLimiterManager(RateLimitingConfig rateLimitingConfig) {
        this.rateLimitingConfig = rateLimitingConfig;
        this.globalBucket = rateLimitingConfig.createGlobalBucket();
    }

    /**
     * Check if request is allowed for a specific user
     */
    public boolean allowRequest(String userId) {
        if (!rateLimitingConfig.isRateLimitEnabled()) {
            return true;
        }

        // Check global bucket
        if (!globalBucket.tryConsume(1)) {
            log.warn("Global rate limit exceeded");
            return false;
        }

        // Check per-user bucket
        Bucket userBucket = userBuckets.computeIfAbsent(userId, k -> rateLimitingConfig.createPerUserBucket());
        if (!userBucket.tryConsume(1)) {
            log.warn("Rate limit exceeded for user: {}", userId);
            return false;
        }

        return true;
    }

    /**
     * Check if request is allowed for a specific endpoint
     */
    public boolean allowEndpointRequest(String userId, String endpoint, int requests, int durationSeconds) {
        if (!rateLimitingConfig.isRateLimitEnabled()) {
            return true;
        }

        String key = userId + ":" + endpoint;
        Bucket endpointBucket = userBuckets.computeIfAbsent(key, k -> rateLimitingConfig.createEndpointBucket(requests, durationSeconds));

        if (!endpointBucket.tryConsume(1)) {
            log.warn("Rate limit exceeded for endpoint: {} by user: {}", endpoint, userId);
            return false;
        }

        return true;
    }

    /**
     * Get remaining tokens for a user
     */
    public long getRemainingTokens(String userId) {
        Bucket userBucket = userBuckets.get(userId);
        if (userBucket == null) {
            return rateLimitingConfig.createPerUserBucket().estimateAbilityToConsume(1).getRoundedTokensToConsume();
        }
        return userBucket.estimateAbilityToConsume(1).getRoundedTokensToConsume();
    }

    /**
     * Reset rate limiter for a user (admin only)
     */
    public void resetUserRateLimit(String userId) {
        userBuckets.remove(userId);
        log.info("Rate limit reset for user: {}", userId);
    }
}
@Component
@ConfigurationProperties(prefix = "app.ratelimit")
@Slf4j
class RateLimitingProperties {
    private boolean enabled = true;
    private Global global = new Global();
    private PerUser perUser = new PerUser();
    private Map<String, PerEndpoint> perEndpoint = new ConcurrentHashMap<>();

    // Getters and Setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Global getGlobal() {
        return global;
    }

    public void setGlobal(Global global) {
        this.global = global;
    }

    public PerUser getPerUser() {
        return perUser;
    }

    public void setPerUser(PerUser perUser) {
        this.perUser = perUser;
    }

    public Map<String, PerEndpoint> getPerEndpoint() {
        return perEndpoint;
    }

    public void setPerEndpoint(Map<String, PerEndpoint> perEndpoint) {
        this.perEndpoint = perEndpoint;
    }

    @Slf4j
    public static class Global {
        private int requests = 1000;
        private int durationSeconds = 3600;

        public int getRequests() {
            return requests;
        }

        public void setRequests(int requests) {
            this.requests = requests;
        }

        public int getDurationSeconds() {
            return durationSeconds;
        }

        public void setDurationSeconds(int durationSeconds) {
            this.durationSeconds = durationSeconds;
        }
    }

    @Slf4j
    public static class PerUser {
        private int requests = 100;
        private int durationSeconds = 3600;

        public int getRequests() {
            return requests;
        }

        public void setRequests(int requests) {
            this.requests = requests;
        }

        public int getDurationSeconds() {
            return durationSeconds;
        }

        public void setDurationSeconds(int durationSeconds) {
            this.durationSeconds = durationSeconds;
        }
    }

    @Slf4j
    public static class PerEndpoint {
        private int requests = 50;
        private int durationSeconds = 3600;

        public int getRequests() {
            return requests;
        }

        public void setRequests(int requests) {
            this.requests = requests;
        }

        public int getDurationSeconds() {
            return durationSeconds;
        }

        public void setDurationSeconds(int durationSeconds) {
            this.durationSeconds = durationSeconds;
        }
    }
}