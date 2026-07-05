package io.github.dmitriyiliyov.ipratelimiter;

import io.lettuce.core.RedisException;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;
import java.util.Objects;

@Slf4j
@Builder
public class RedisRateLimitRepository implements RateLimitRepository {

    private final RedisTemplate<String, Boolean> redisTemplate;
    private final DefaultRedisScript<Boolean> script;
    private final String targetUrl;
    private final String keyTemplate;
    private final Long maxAttemptCount;
    private final Long observeTime;
    private final Long lockTime;

    public RedisRateLimitRepository(RedisTemplate<String, Boolean> redisTemplate,
                                    DefaultRedisScript<Boolean> script,
                                    String targetUrl,
                                    String keyTemplate,
                                    Long maxAttemptCount,
                                    Long observeTime,
                                    Long lockTime) {
        Objects.requireNonNull(redisTemplate, "redisTemplate must not be null");
        Objects.requireNonNull(script, "script must not be null");
        Objects.requireNonNull(targetUrl, "targetUrl must not be null");
        Objects.requireNonNull(keyTemplate, "keyTemplate must not be null");
        Objects.requireNonNull(maxAttemptCount, "maxAttemptCount must not be null");
        Objects.requireNonNull(observeTime, "observeTime must not be null");
        Objects.requireNonNull(lockTime, "lockTime must not be null");
        this.redisTemplate = redisTemplate;
        this.script = script;
        this.targetUrl = targetUrl;
        this.keyTemplate = keyTemplate + ":ip:%s";
        this.maxAttemptCount = maxAttemptCount;
        this.observeTime = observeTime;
        this.lockTime = lockTime;
        log.info("Create RedisRateLimitRepository with target url: {}", this.targetUrl);
    }

    @Override
    public String getTargetUrl() {
        return targetUrl;
    }

    @Override
    public RateLimitStatus increment(String ip) {
        try {
            Boolean result = redisTemplate.execute(
                    script,
                    List.of(keyTemplate.formatted(ip), "blocked:" + keyTemplate.formatted(ip)),
                    String.valueOf(observeTime),
                    String.valueOf(lockTime),
                    String.valueOf(maxAttemptCount)
            );
            return RateLimitStatus.fromBoolean(result);
        } catch (RedisException e) {
            log.error("Redis exception when incrementing limit", e);
            if (e.getMessage() != null && e.getMessage().contains("NOSCRIPT")) {
                log.warn("Script wasn't load, exception contains NOSCRIPT");
            }
            throw e;
        } catch (Exception e) {
            log.error("Error when incrementing limit", e);
            throw e;
        }
    }
}
