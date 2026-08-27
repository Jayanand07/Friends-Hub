package com.example.socialmedia.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class KeepAliveScheduler {

    private static final Logger log =
        LoggerFactory.getLogger(KeepAliveScheduler.class);

    @Scheduled(fixedDelay = 840000)
    public void keepAlive() {
        log.debug("Keep-alive ping - preventing Render cold start");
    }
}
