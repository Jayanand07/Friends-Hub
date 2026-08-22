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
 * Rate Limiting Filter using a sliding-window counter.
 *
 * - Dedicated per-IP rate limiting for auth endpoints vs general traffic
 * - Bypasses CORS preflight OPTIONS requests
 * - Configurable via application.properties
 * - Returns proper 429 JSON response with Retry-After header
 * - Skips actuator & health-check endpoints
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    /** Stores general request count + window start per IP */
    private final Map<String, RateLimitEntry> ipCounters = new ConcurrentHashMap<>();

    /** Stores auth-specific request count + window start per IP */
    private final Map<String, RateLimitEntry> authCounters = new ConcurrentHashMap<>();

    @Value("${app.ratelimit.enabled:true}")
    private boolean enabled;

    @Value("${app.ratelimit.requests-per-minute:120}")
    private int requestsPerMinute;

    @Value("${app.ratelimit.auth.requests-per-minute:25}")
    private int authRequestsPerMinute;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        if (!enabled || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        String requestPath = request.getRequestURI();
        boolean isAuthEndpoint = requestPath.startsWith("/api/auth/login")
            || requestPath.startsWith("/api/auth/register")
            || requestPath.startsWith("/api/auth/forgot-password")
            || requestPath.startsWith("/api/auth/reset-password")
            || requestPath.startsWith("/api/auth/resend-verification");

        long now = System.currentTimeMillis();

        if (isAuthEndpoint) {
            RateLimitEntry authEntry = authCounters.computeIfAbsent(clientIp, k -> new RateLimitEntry());
            int currentAuthCount;
            synchronized (authEntry) {
                if (now - authEntry.windowStart.get() > 60_000L) {
                    authEntry.count.set(0);
                    authEntry.windowStart.set(now);
                }
                currentAuthCount = authEntry.count.incrementAndGet();
            }

            if (authCounters.size() > 1000) {
                authCounters.entrySet().removeIf(e -> (now - e.getValue().windowStart.get()) > 60_000L);
            }

            if (currentAuthCount > authRequestsPerMinute) {
                log.warn("Auth rate limit exceeded for IP: {} on endpoint: {} {}",
                        clientIp, request.getMethod(), request.getRequestURI());
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.addHeader("Retry-After", "60");
                response.getWriter().write("""
                        {
                          "status": 429,
                          "error": "Too Many Requests",
                          "message": "Too many authentication attempts. Please try again in 1 minute."
                        }
                        """);
                return;
            }

            response.addHeader("X-RateLimit-Remaining",
                    String.valueOf(Math.max(0, authRequestsPerMinute - currentAuthCount)));
        } else {
            RateLimitEntry entry = ipCounters.computeIfAbsent(clientIp, k -> new RateLimitEntry());
            int currentCount;
            synchronized (entry) {
                if (now - entry.windowStart.get() > 60_000L) {
                    entry.count.set(0);
                    entry.windowStart.set(now);
                }
                currentCount = entry.count.incrementAndGet();
            }

            if (ipCounters.size() > 1000) {
                ipCounters.entrySet().removeIf(e -> (now - e.getValue().windowStart.get()) > 60_000L);
            }

            if (currentCount > requestsPerMinute) {
                log.warn("General rate limit exceeded for IP: {} on endpoint: {} {}",
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

            response.addHeader("X-RateLimit-Remaining",
                    String.valueOf(Math.max(0, requestsPerMinute - currentCount)));
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator") || "OPTIONS".equalsIgnoreCase(request.getMethod());
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
