package com.timerfy.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.timerfy.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class RateLimitingConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitingConfig.class);
    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";
    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private static final int WINDOW_SIZE_MINUTES = 1;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Around("@annotation(rateLimited)")
    public Object rateLimit(ProceedingJoinPoint joinPoint, RateLimited rateLimited) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }
        
        HttpServletRequest request = attributes.getRequest();
        String clientIp = getClientIpAddress(request);
        String endpoint = request.getRequestURI();
        String key = RATE_LIMIT_KEY_PREFIX + clientIp + ":" + endpoint;
        
        // Get current request count
        String currentCountStr = redisTemplate.opsForValue().get(key);
        int currentCount = currentCountStr != null ? Integer.parseInt(currentCountStr) : 0;
        
        int maxRequests = rateLimited.maxRequests() > 0 ? rateLimited.maxRequests() : MAX_REQUESTS_PER_MINUTE;
        
        if (currentCount >= maxRequests) {
            logger.warn("Rate limit exceeded for IP {} on endpoint {}: {}/{} requests", 
                       clientIp, endpoint, currentCount, maxRequests);
            
            ApiResponse<Object> response = ApiResponse.error(
                "RATE_LIMIT_EXCEEDED", 
                "Too many requests. Please try again later."
            );
            
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
        }
        
        // Increment counter
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, Duration.ofMinutes(WINDOW_SIZE_MINUTES));
        
        return joinPoint.proceed();
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedForHeader = request.getHeader("X-Forwarded-For");
        if (xForwardedForHeader != null && !xForwardedForHeader.isEmpty()) {
            return xForwardedForHeader.split(",")[0].trim();
        }
        
        String xRealIpHeader = request.getHeader("X-Real-IP");
        if (xRealIpHeader != null && !xRealIpHeader.isEmpty()) {
            return xRealIpHeader;
        }
        
        return request.getRemoteAddr();
    }
}