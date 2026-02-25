package com.example.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.time.Duration;

@Configuration
public class CircuitBreakerConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerConfiguration.class);

    @Bean
    public CircuitBreakerConfig chatServiceCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(10))
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .build();
    }

    @Bean
    public CircuitBreakerConfig notificationServiceCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(10))
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .build();
    }

    @Bean
    public CircuitBreakerConfig mediaUploadServiceCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(10))
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .build();
    }
}