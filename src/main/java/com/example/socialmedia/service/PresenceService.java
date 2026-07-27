package com.example.socialmedia.service;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

@Service
public class PresenceService {

    private final RedisTemplate<String, String> redisTemplate;
    public PresenceService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    private static final String ONLINE_PREFIX = "presence:online:";
    private static final Duration ONLINE_TTL = Duration.ofSeconds(30);

    public void setUserOnline(Long userId) {
        redisTemplate.opsForValue().set(ONLINE_PREFIX + userId, "1", ONLINE_TTL);
    }

    public void setUserOffline(Long userId) {
        redisTemplate.delete(ONLINE_PREFIX + userId);
    }

    public boolean isUserOnline(Long userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(ONLINE_PREFIX + userId));
    }

    public Set<Long> getOnlineUsers(List<Long> userIds) {
        Set<Long> online = new HashSet<>();
        for (Long userId : userIds) {
            if (Boolean.TRUE.equals(redisTemplate.hasKey(ONLINE_PREFIX + userId))) {
                online.add(userId);
            }
        }
        return online;
    }

    /**
     * Return all currently online user IDs by scanning Redis keys with the presence prefix.
     * Uses SCAN instead of KEYS to avoid blocking the Redis server on large key sets.
     */
    public Set<Long> getAllOnlineUserIds() {
        Set<Long> userIds = new HashSet<>();
        ScanOptions scanOptions = ScanOptions.scanOptions().match(ONLINE_PREFIX + "*").count(100).build();
        try (Cursor<String> cursor = redisTemplate.scan(scanOptions)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                try {
                    userIds.add(Long.valueOf(key.substring(ONLINE_PREFIX.length())));
                } catch (NumberFormatException ignored) {
                }
            }
        } catch (Exception e) {
            // Fallback: use keys if SCAN is not supported
            Set<String> keys = redisTemplate.keys(ONLINE_PREFIX + "*");
            if (keys != null) {
                for (String key : keys) {
                    try {
                        userIds.add(Long.valueOf(key.substring(ONLINE_PREFIX.length())));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return userIds;
    }
}
