package com.example.socialmedia.config;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Component
public class WebMvcConfig implements WebMvcConfigurer {

    private final ApiMetricsInterceptor apiMetricsInterceptor;

    public WebMvcConfig(ApiMetricsInterceptor apiMetricsInterceptor) {
        this.apiMetricsInterceptor = apiMetricsInterceptor;
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(apiMetricsInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/actuator/**");
    }

    // NOTE: the custom PageableHandlerMethodArgumentResolver that was registered
    // here could be shadowed by Spring Boot's auto-registered one (resolver order
    // is not guaranteed), so the intended max-page-size=100 cap was not reliably
    // applied. The cap is now set declaratively via
    // spring.data.web.pageable.max-page-size=100 in application.properties,
    // which configures Spring Boot's own resolver.
}
