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
        // Validate token on SEND frame
        else if (StompCommand.SEND.equals(accessor.getCommand())) {
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
