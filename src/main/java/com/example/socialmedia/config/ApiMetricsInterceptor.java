package com.example.socialmedia.config;

import com.example.socialmedia.entity.ApiUsageLog;
import com.example.socialmedia.repository.ApiUsageLogRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

/**
 * Interceptor that records API usage metrics for EVERY request.
 *
 * What it tracks (answering "how to know all this"):
 * - Total request count per endpoint (Prometheus counter)
 * - Response time per endpoint (Prometheus histogram)
 * - Error count per endpoint (Prometheus counter)
 * - Rate-limit breach count (Prometheus counter)
 * - Full request log in the database (ApiUsageLog entity)
 *
 * Where: Registered via WebMvcConfig for all /api/** paths
 * Which APIs: Every API endpoint automatically — no per-controller code needed
 */
@Component
public class ApiMetricsInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ApiMetricsInterceptor.class);

    private final MeterRegistry meterRegistry;
    private final ApiUsageLogRepository usageLogRepository;
    private static final String START_TIME_ATTR = "apiMetrics.startTime";

    public ApiMetricsInterceptor(MeterRegistry meterRegistry,
            ApiUsageLogRepository usageLogRepository) {
        this.meterRegistry = meterRegistry;
        this.usageLogRepository = usageLogRepository;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler) {
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());

        // Increment total request counter
        meterRegistry.counter("api.requests.total",
                "endpoint", request.getRequestURI(),
                "method", request.getMethod()).increment();

        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler, @Nullable Exception ex) {
        try {
            Long startTime = (Long) request.getAttribute(START_TIME_ATTR);
            if (startTime == null)
                return;

            long durationMs = System.currentTimeMillis() - startTime;
            String endpoint = request.getRequestURI();
            String method = request.getMethod();
            int statusCode = response.getStatus();

            // ---- Prometheus metrics ----
            Timer.builder("api.response.time")
                    .description("API response time")
                    .tag("endpoint", endpoint)
                    .tag("method", method)
                    .tag("status", String.valueOf(statusCode))
                    .register(meterRegistry)
                    .record(durationMs, TimeUnit.MILLISECONDS);

            if (statusCode >= 400) {
                meterRegistry.counter("api.errors.total",
                        "endpoint", endpoint,
                        "status", String.valueOf(statusCode)).increment();
            }

            if (statusCode == 429) {
                meterRegistry.counter("api.ratelimit.exceeded",
                        "endpoint", endpoint).increment();
            }

            if (ex != null) {
                meterRegistry.counter("api.exceptions.total",
                        "exception", ex.getClass().getSimpleName(),
                        "endpoint", endpoint).increment();
            }

            // ---- Database usage log (indexing) ----
            String userId = getCurrentUserId();
            String clientIp = request.getHeader("X-Forwarded-For");
            if (clientIp == null)
                clientIp = request.getRemoteAddr();

            ApiUsageLog logEntry = new ApiUsageLog(
                    userId, method, endpoint, statusCode, durationMs, clientIp);
            if (ex != null) {
                logEntry.setErrorMessage(ex.getMessage());
            }

            // Save asynchronously; if DB is down this shouldn't crash the request
            try {
                org.springframework.scheduling.annotation.Async.class.getName(); // compile-time reference
                saveLogAsync(logEntry);
            } catch (Exception dbEx) {
                log.warn("Failed to persist API usage log: {}", dbEx.getMessage());
            }

        } catch (Exception e) {
            log.error("Error in ApiMetricsInterceptor.afterCompletion", e);
        }
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return null;
    }

    /**
     * M-3: Save API usage log asynchronously so it doesn't block the response thread.
     */
    private void saveLogAsync(ApiUsageLog logEntry) {
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                usageLogRepository.save(logEntry);
            } catch (Exception e) {
                log.warn("Async API log save failed: {}", e.getMessage());
            }
        });
    }
}
