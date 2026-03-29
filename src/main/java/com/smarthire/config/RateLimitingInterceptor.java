
package com.smarthire.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class RateLimitingInterceptor implements HandlerInterceptor {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private static final String RATE_LIMIT_PREFIX = "api:ratelimit:";

    public RateLimitingInterceptor(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String userId = request.getHeader("X-User-Id");
        if (userId == null) {
            return true; // Allow if no user ID
        }

        String key = RATE_LIMIT_PREFIX + userId + ":" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));

        Integer count = (Integer) redisTemplate.opsForValue().get(key);

        if (count == null) {
            redisTemplate.opsForValue().set(key, 1, 1, TimeUnit.MINUTES);
            return true;
        }

        if (count >= MAX_REQUESTS_PER_MINUTE) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Rate limit exceeded. Please try again later.\"}");
            log.warn("Rate limit exceeded for user: {}", userId);
            return false;
        }

        redisTemplate.opsForValue().increment(key);
        return true;
    }
}