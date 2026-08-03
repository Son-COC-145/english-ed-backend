package com.example.english_app.service.integration;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiRateLimiterService {
    private final StringRedisTemplate redisTemplate;

    public boolean checkAndIncrementUsage(String userEmail, int limit) {
        String today = LocalDate.now().toString();
        String key = "ai_usage:user:" + userEmail + ":date:" + today;

        Long currentUsage = redisTemplate.opsForValue().increment(key);
        if (currentUsage == null) {
            return false;
        }
        if (currentUsage == 1) {
            redisTemplate.expire(key, 24, TimeUnit.HOURS);
        }

        log.info("User {} usage: {}/{} requests today", userEmail, currentUsage, limit);
        return currentUsage <= limit;
    }
}
