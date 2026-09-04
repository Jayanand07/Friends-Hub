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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class StompHeaderInterceptor implements ChannelInterceptor {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StompHeaderInterceptor.class);

    /**
     * SECURITY (C-3): personal queue destinations look like /queue/messages-<userId>.
     * The broker delivers to ANY subscriber of a literal destination, so without
     * a check here any authenticated user could subscribe to another user's
     * queues and eavesdrop on their messages, typing events and notifications.
     */
    private static final Pattern PERSONAL_QUEUE_PATTERN =
            Pattern.compile("^/queue/([a-zA-Z]+)-(\\d+)$");
    private static final Pattern GROUP_TOPIC_PATTERN =
            Pattern.compile("^/topic/group-(\\d+)$");

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final com.example.socialmedia.repository.ChatGroupRepository chatGroupRepository;

    /** sessionId -> authenticated userId, captured at CONNECT time. */
    private final ConcurrentHashMap<String, Long> sessionUserIdMap = new ConcurrentHashMap<>();

    public StompHeaderInterceptor(JwtService jwtService, UserDetailsService userDetailsService,
            com.example.socialmedia.repository.ChatGroupRepository chatGroupRepository) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.chatGroupRepository = chatGroupRepository;
    }

    private final java.util.Map<String, WsRateLimitEntry> wsRateLimits = new ConcurrentHashMap<>();

    private static class WsRateLimitEntry {
        final AtomicInteger count = new AtomicInteger(0);
        final AtomicLong windowStart = new AtomicLong(System.currentTimeMillis());
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
            // SECURITY (C-3): enforce destination-level authorization
            authorizeSubscription(accessor);
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
        // Clean up the session -> user mapping on disconnect
        else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            if (accessor.getSessionId() != null) {
                sessionUserIdMap.remove(accessor.getSessionId());
            }
        }

        return message;
    }

    /**
     * SECURITY (C-3): Any authenticated user could previously subscribe to
     * /queue/messages-<otherUserId> (or any other user's personal queue, or a
     * group topic they are not a member of) and receive that user's live
     * messages, typing events, read receipts and notifications. This method
     * ensures a client can only subscribe to its OWN personal queues and to
     * topics for groups it actually belongs to.
     */
    private void authorizeSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) {
            return;
        }

        String sessionId = accessor.getSessionId();
        Long userId = sessionId != null ? sessionUserIdMap.get(sessionId) : null;
        if (userId == null) {
            throw new org.springframework.messaging.MessageDeliveryException(
                    "Unknown session — reconnect with a valid token");
        }

        // Personal queues (/queue/messages-123, /queue/typing-123, ...):
        // only the owner may subscribe.
        Matcher personal = PERSONAL_QUEUE_PATTERN.matcher(destination);
        if (personal.matches()) {
            Long destinationUserId = Long.parseLong(personal.group(2));
            if (!destinationUserId.equals(userId)) {
                log.warn("BLOCKED cross-user WebSocket subscription: session {} (user {}) attempted to subscribe to {}",
                        sessionId, userId, destination);
                throw new org.springframework.messaging.MessageDeliveryException(
                        "Subscription not permitted: destination belongs to another user");
            }
            return;
        }

        // Group topics (/topic/group-42): requires actual membership.
        Matcher group = GROUP_TOPIC_PATTERN.matcher(destination);
        if (group.matches()) {
            Long groupId = Long.parseLong(group.group(1));
            boolean isMember = chatGroupRepository.findByIdAndMembers_Id(groupId, userId).isPresent();
            if (!isMember) {
                log.warn("BLOCKED non-member WebSocket subscription: session {} (user {}) attempted to subscribe to {}",
                        sessionId, userId, destination);
                throw new org.springframework.messaging.MessageDeliveryException(
                        "Subscription not permitted: not a member of this group");
            }
            return;
        }

        // Everything else (e.g. /topic/online-users, /user/queue/**) is either
        // a public broadcast or routed by the broker's per-user destination
        // resolver (which only delivers to the authenticated user's sessions).
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

                        // Track which userId owns this session so we can
                        // authorize its subscriptions later (C-3).
                        Object userIdClaim = jwtService.extractClaim(jwt, c -> c.get("userId"));
                        if (userIdClaim instanceof Number n && accessor.getSessionId() != null) {
                            sessionUserIdMap.put(accessor.getSessionId(), n.longValue());
                        }
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
