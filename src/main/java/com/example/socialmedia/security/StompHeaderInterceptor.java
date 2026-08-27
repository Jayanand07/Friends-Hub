package com.example.socialmedia.security;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

@Component
public class StompHeaderInterceptor implements ChannelInterceptor {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StompHeaderInterceptor.class);

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public StompHeaderInterceptor(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    private final java.util.Map<String, WsRateLimitEntry> wsRateLimits = new java.util.concurrent.ConcurrentHashMap<>();

    private static class WsRateLimitEntry {
        final java.util.concurrent.atomic.AtomicInteger count = new java.util.concurrent.atomic.AtomicInteger(0);
        final java.util.concurrent.atomic.AtomicLong windowStart = new java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis());
    }

    private boolean isWsRateLimited(String sessionId) {
        if (sessionId == null) return false;
        long now = System.currentTimeMillis();
        WsRateLimitEntry entry = wsRateLimits.computeIfAbsent(sessionId, k -> new WsRateLimitEntry());
        synchronized (entry) {
            if (now - entry.windowStart.get() > 60_000L) {
                entry.count.set(0);
                entry.windowStart.set(now);
            }
            int current = entry.count.incrementAndGet();
            if (wsRateLimits.size() > 5000) {
                wsRateLimits.entrySet().removeIf(e -> (now - e.getValue().windowStart.get()) > 60_000L);
            }
            return current > 60; // Max 60 STOMP messages per minute per session
        }
    }

    @Override
    public Message<?> preSend(@org.springframework.lang.NonNull Message<?> message,
            @org.springframework.lang.NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        // Validate token on CONNECT frame - initial WebSocket handshake
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            validateAndSetAuthentication(accessor);
        }
        // Validate token on SUBSCRIBE frame - allow user channel subscription
        else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            if (accessor.getUser() != null) {
                SecurityContextHolder.getContext().setAuthentication((org.springframework.security.core.Authentication) accessor.getUser());
            } else {
                validateAndSetAuthentication(accessor);
            }
        }
        // Validate token and rate-limit on SEND frame
        else if (StompCommand.SEND.equals(accessor.getCommand())) {
            if (isWsRateLimited(accessor.getSessionId())) {
                log.warn("WebSocket message rate limit exceeded for session: {}", accessor.getSessionId());
                throw new org.springframework.messaging.MessageDeliveryException("Rate limit exceeded: maximum 60 messages per minute");
            }
            if (accessor.getUser() != null) {
                SecurityContextHolder.getContext().setAuthentication((org.springframework.security.core.Authentication) accessor.getUser());
            } else {
                validateAndSetAuthentication(accessor);
            }
        }

        return message;
    }

    private void validateAndSetAuthentication(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);
            String userEmail = jwtService.extractUsername(jwt);

            if (userEmail != null) {
                try {
                    // Reject blacklisted tokens at the STOMP level
                    if (jwtService.isTokenBlacklisted(jwt)) {
                        log.warn("WebSocket connection rejected: token is blacklisted for user {}", maskEmail(userEmail));
                        throw new org.springframework.messaging.MessageDeliveryException("Token has been revoked");
                    }

                    UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                    if (jwtService.isTokenValid(jwt, userDetails)) {
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        accessor.setUser(authToken);
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    } else {
                        log.warn("WebSocket auth failed: invalid or expired token for user {}", maskEmail(userEmail));
                        throw new org.springframework.messaging.MessageDeliveryException("Invalid or expired token");
                    }
                } catch (org.springframework.messaging.MessageDeliveryException e) {
                    throw e;
                } catch (Exception e) {
                    log.warn("WebSocket authentication failed: {}", e.getMessage());
                    throw new org.springframework.messaging.MessageDeliveryException("Authentication failed: " + e.getMessage());
                }
            } else {
                throw new org.springframework.messaging.MessageDeliveryException("Invalid token: no subject");
            }
        } else {
            // CONNECT without Bearer token — reject
            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                throw new org.springframework.messaging.MessageDeliveryException("Missing or invalid Authorization header");
            }
        }
    }

    /** Mask email for logging: e.g. "user@example.com" -> "u*****@example.com" */
    private static String maskEmail(String email) {
        if (email == null || email.isBlank()) return email;
        int atIdx = email.indexOf('@');
        if (atIdx <= 1) return email;
        return email.charAt(0) + "*****" + email.substring(atIdx);
    }
}
