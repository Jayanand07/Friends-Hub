package com.example.socialmedia.scheduler;

import com.example.socialmedia.service.StoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class StoryCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(StoryCleanupScheduler.class);

    private final StoryService storyService;

    public StoryCleanupScheduler(StoryService storyService) {
        this.storyService = storyService;
    }

    @Scheduled(fixedRate = 3600000) // every hour
    public void cleanupExpiredStories() {
        log.info("Running story cleanup task...");
        storyService.cleanupExpiredStories();
        log.info("Story cleanup complete.");
    }
}
