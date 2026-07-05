package io.github.dmitriyiliyov.ipratelimiter.config;

import io.github.dmitriyiliyov.ipratelimiter.RateLimitRepository;
import io.github.dmitriyiliyov.ipratelimiter.RedisRateLimitRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;

@Configuration
public class RateLimiterConfig {

    @Bean
    public RateLimitRepository rateLimitRepository(RedisTemplate<String, Boolean> redisTemplate,
                                                   DefaultRedisScript<Boolean> script) {
        return RedisRateLimitRepository.builder()
                .redisTemplate(redisTemplate)
                .script(script)
                .targetUrl("/api/test-ip-rate-limiter/guarded")
                .keyTemplate("rate-limit")
                .observeTime(Duration.ofSeconds(10).getSeconds())
                .lockTime(Duration.ofMinutes(15).getSeconds())
                .maxAttemptCount(3L)
                .build();
    }
}
