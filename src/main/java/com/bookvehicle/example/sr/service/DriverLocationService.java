package com.bookvehicle.example.sr.service;

import com.bookvehicle.example.sr.dto.DriverLocationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class DriverLocationService {

    private static final Duration TTL = Duration.ofSeconds(300);
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public DriverLocationService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void saveLocation(String tripType, Long tripId, DriverLocationResponse payload) {
        try {
            String key = key(tripType, tripId);
            String value = objectMapper.writeValueAsString(payload);
            redisTemplate.opsForValue().set(key, value, TTL);
        } catch (Exception ignored) {
        }
    }

    public DriverLocationResponse getLocation(String tripType, Long tripId) {
        try {
            String raw = redisTemplate.opsForValue().get(key(tripType, tripId));
            if (raw == null || raw.isBlank()) {
                return null;
            }
            return objectMapper.readValue(raw, DriverLocationResponse.class);
        } catch (Exception e) {
            return null;
        }
    }

    private String key(String tripType, Long tripId) {
        return "driver:loc:" + tripType + ":" + tripId;
    }
}
