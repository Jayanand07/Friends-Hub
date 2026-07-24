package com.example.socialmedia.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Rate Limiting Filter using a sliding-window counter (no external libraries).
 *
 * - Per-IP rate limiting to prevent DDoS / brute force
 * - Configurable via application.properties
 * - Returns proper 429 JSON response with Retry-After header
 * - Adds X-RateLimit-Remaining header for API visibility
 * - Skips actuator & health-check endpoints
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    /** Stores request count + window start per IP */
    private final Map<String, RateLimitEntry> ipCounters = new ConcurrentHashMap<>();

    @Value("${app.ratelimit.enabled:true}")
    private boolean enabled;

    @Value("${app.ratelimit.requests-per-minute:60}")
    private int requestsPerMinute;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        RateLimitEntry entry = ipCounters.computeIfAbsent(clientIp, k -> new RateLimitEntry());

        // Reset counter or clean up stale entries if 1-minute window has elapsed
        long now = System.currentTimeMillis();
        if (now - entry.windowStart.get() > 60_000L) {
            entry.count.set(0);
            entry.windowStart.set(now);
        }

        // Bounded map maintenance: purge stale entries if map size exceeds threshold
        if (ipCounters.size() > 1000) {
            ipCounters.entrySet().removeIf(e -> (now - e.getValue().windowStart.get()) > 60_000L);
        }

        int currentCount = entry.count.incrementAndGet();

        // Stricter rate limit for authentication endpoints to prevent brute force
        String requestPath = request.getRequestURI();
        boolean isAuthEndpoint = requestPath.startsWith("/api/auth/login")
            || requestPath.startsWith("/api/auth/register")
            || requestPath.startsWith("/api/auth/forgot-password")
            || requestPath.startsWith("/api/auth/reset-password");
        int effectiveLimit = isAuthEndpoint ? Math.min(requestsPerMinute, 10) : requestsPerMinute;

        if (currentCount > effectiveLimit) {
            log.warn("Rate limit exceeded for IP: {} on endpoint: {} {}",
                    clientIp, request.getMethod(), request.getRequestURI());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.addHeader("Retry-After", "60");
            response.getWriter().write("""
                    {
                      "status": 429,
                      "error": "Too Many Requests",
                      "message": "Rate limit exceeded. Please try again later."
                    }
                    """);
            return;
        }

        // Add rate-limit headers for API visibility
        response.addHeader("X-RateLimit-Remaining",
                String.valueOf(Math.max(0, requestsPerMinute - currentCount)));
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator");
    }

    // Trusted internal proxy CIDR prefixes — only trust X-Forwarded-For from these
    private static final java.util.Set<String> TRUSTED_PROXY_PREFIXES = java.util.Set.of(
        "10.", "172.16.", "172.17.", "172.18.", "172.19.", "172.20.",
        "172.21.", "172.22.", "172.23.", "172.24.", "172.25.", "172.26.",
        "172.27.", "172.28.", "172.29.", "172.30.", "172.31.", "192.168.", "127."
    );

    private String getClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        boolean isTrustedProxy = TRUSTED_PROXY_PREFIXES.stream()
            .anyMatch(remoteAddr::startsWith);
        if (isTrustedProxy) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                return xff.split(",")[0].trim();
            }
        }
        return remoteAddr;
    }

    /** Simple rate limit tracking per IP */
    private static class RateLimitEntry {
        final AtomicInteger count = new AtomicInteger(0);
        final AtomicLong windowStart = new AtomicLong(System.currentTimeMillis());
    }
}
