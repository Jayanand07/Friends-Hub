package com.example.socialmedia.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final com.example.socialmedia.security.StompHeaderInterceptor stompInterceptor;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    public WebSocketConfig(com.example.socialmedia.security.StompHeaderInterceptor stompInterceptor) {
        this.stompInterceptor = stompInterceptor;
    }

    @Override
    public void configureMessageBroker(@org.springframework.lang.NonNull MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(@org.springframework.lang.NonNull StompEndpointRegistry registry) {
        List<String> origins = new ArrayList<>(List.of(
                "http://localhost:5173",
                "http://localhost:3000"
        ));
        if (frontendUrl != null && !frontendUrl.isBlank()) {
            origins.add(frontendUrl);
        }
        registry.addEndpoint("/ws")
                .setAllowedOrigins(origins.toArray(new String[0]))
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(
            @org.springframework.lang.NonNull org.springframework.messaging.simp.config.ChannelRegistration registration) {
        registration.interceptors(stompInterceptor);
    }
}
