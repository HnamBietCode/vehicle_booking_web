package com.bookvehicle.example.sr.service;

import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

@Service
public class RealtimeStatsService {

    private final StringRedisTemplate redisTemplate;

    public RealtimeStatsService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public long countActiveDriverLocations() {
        try {
            Long result = redisTemplate.execute((RedisCallback<Long>) connection -> {
                AtomicLong count = new AtomicLong(0);
                try (var cursor = connection.scan(ScanOptions.scanOptions().match("driver:loc:*").count(1000).build())) {
                    cursor.forEachRemaining(k -> count.incrementAndGet());
                }
                return count.get();
            });
            return result == null ? 0 : result;
        } catch (Exception e) {
            return 0;
        }
    }
}
