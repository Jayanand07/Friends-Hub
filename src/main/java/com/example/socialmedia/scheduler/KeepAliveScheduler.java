package com.example.socialmedia.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * PERFORMANCE: Render's free tier idles a service after ~15 minutes without
 * INBOUND traffic, and every request during a cold start can wait 30-60s —
 * this is the single biggest source of "the app is very slow" reports.
 *
 * The previous implementation only logged a message (no request = no inbound
 * traffic = no keep-alive). This version issues a real HTTP GET to the
 * service's PUBLIC health endpoint:
 *   - on Render, RENDER_EXTERNAL_URL is injected automatically and requests to
 *     it count as real inbound traffic, preventing idle sleeps;
 *   - elsewhere, set APP_PUBLIC_URL (e.g. https://your-api.onrender.com);
 *   - if neither is set, we ping localhost (dev) which at least warms the JVM.
 */
@Component
public class KeepAliveScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(KeepAliveScheduler.class);

    @Value("${app.public-url:}")
    private String configuredPublicUrl;

    @Value("${server.port:8080}")
    private String serverPort;

    // 10 minutes — safely inside Render's ~15 minute idle window.
    @Scheduled(fixedDelay = 600_000)
    public void keepAlive() {
        String url = resolvePublicUrl();
        ping(url);
    }

    private String resolvePublicUrl() {
        // Render injects RENDER_EXTERNAL_URL automatically in service environments.
        String renderUrl = System.getenv("RENDER_EXTERNAL_URL");
        if (renderUrl != null && !renderUrl.isBlank()) {
            return renderUrl;
        }
        if (configuredPublicUrl != null && !configuredPublicUrl.isBlank()) {
            return configuredPublicUrl;
        }
        log.warn("Neither RENDER_EXTERNAL_URL nor APP_PUBLIC_URL is set — keep-alive "
                + "pings localhost only, which does NOT prevent Render free-tier idle "
                + "sleeps. Set APP_PUBLIC_URL to your public backend URL to avoid slow "
                + "cold starts.");
        return "http://localhost:" + serverPort;
    }

    private void ping(String baseUrl) {
        try {
            String url = baseUrl.replaceAll("/+$", "") + "/actuator/health";
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection)
                    new java.net.URL(url).openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestMethod("GET");
            int status = conn.getResponseCode();
            conn.disconnect();
            log.debug("Keep-alive ping {} -> {}", url, status);
        } catch (Exception e) {
            log.warn("Keep-alive ping failed: {}", e.getMessage());
        }
    }
}
