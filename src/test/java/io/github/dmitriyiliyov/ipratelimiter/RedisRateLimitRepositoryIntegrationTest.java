package io.github.dmitriyiliyov.ipratelimiter;

import org.junit.jupiter.api.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scripting.support.ResourceScriptSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class RedisRateLimitRepositoryIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    static LettuceConnectionFactory connectionFactory;
    static RedisTemplate<String, Boolean> redisTemplate;
    static DefaultRedisScript<Boolean> script;

    RedisRateLimitRepository tested;

    String targetUrl;
    String ip;
    long observeTime;
    long lockTime;
    long maxAttemptCount;

    @BeforeAll
    static void setUpInfrastructure() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(
                redis.getHost(), redis.getMappedPort(6379));
        connectionFactory = new LettuceConnectionFactory(config);
        connectionFactory.afterPropertiesSet();

        redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        redisTemplate.afterPropertiesSet();

        script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("rate_limit_script.lua")));
        script.setResultType(Boolean.class);
    }

    @AfterAll
    static void tearDownInfrastructure() {
        connectionFactory.destroy();
    }

    @BeforeEach
    void setUp() {
        targetUrl = "/api/test-ip-rate-limiter/guarded";
        ip = "192.168.0.1";
        observeTime = 10L;
        lockTime = 900L;
        maxAttemptCount = 3L;

        tested = RedisRateLimitRepository.builder()
                .redisTemplate(redisTemplate)
                .script(script)
                .targetUrl(targetUrl)
                .keyTemplate("rate-limit")
                .maxAttemptCount(maxAttemptCount)
                .observeTime(observeTime)
                .lockTime(lockTime)
                .build();
    }

    @AfterEach
    void cleanRedis() {
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushAll();
            return null;
        });
    }

    @Test
    @DisplayName("IT getTargetUrl() should return configured targetUrl")
    void getTargetUrl_shouldReturnConfiguredTargetUrl() {
        assertThat(tested.getTargetUrl()).isEqualTo(targetUrl);
    }

    @Test
    @DisplayName("IT increment() when first request, should return ALLOWED and set TTL on counter key")
    void increment_whenFirstRequest_shouldReturnAllowedAndSetTtlOnCounterKey() {
        // when
        RateLimitStatus status = tested.increment(ip);

        // then
        assertThat(status).isEqualTo(RateLimitStatus.ALLOWED);
        assertThat(redisTemplate.getExpire("rate-limit:ip:" + ip)).isPositive();
    }

    @Test
    @DisplayName("IT increment() when requests within limit, should return ALLOWED for all")
    void increment_whenRequestsWithinLimit_shouldReturnAllowedForAll() {
        // when & then
        for (int i = 0; i < maxAttemptCount; i++) {
            assertThat(tested.increment(ip)).isEqualTo(RateLimitStatus.ALLOWED);
        }
    }

    @Test
    @DisplayName("IT increment() when request exceeds limit, should return BLOCKED and set blocked key with TTL")
    void increment_whenRequestExceedsLimit_shouldReturnBlockedAndSetBlockedKeyWithTtl() {
        // given
        for (int i = 0; i < maxAttemptCount; i++) {
            tested.increment(ip);
        }

        // when
        RateLimitStatus status = tested.increment(ip);

        // then
        assertThat(status).isEqualTo(RateLimitStatus.BLOCKED);
        String blockedKey = "blocked:rate-limit:ip:" + ip;
        assertThat(redisTemplate.hasKey(blockedKey)).isTrue();
        assertThat(redisTemplate.getExpire(blockedKey)).isPositive();
    }

    @Test
    @DisplayName("IT increment() when IP is already blocked, should immediately return BLOCKED")
    void increment_whenIpIsAlreadyBlocked_shouldImmediatelyReturnBlocked() {
        // given
        for (int i = 0; i < maxAttemptCount; i++) {
            tested.increment(ip);
        }
        tested.increment(ip);

        // when
        RateLimitStatus status = tested.increment(ip);

        // then
        assertThat(status).isEqualTo(RateLimitStatus.BLOCKED);
    }

    @Test
    @DisplayName("IT increment() when observe TTL expires, should reset counter and return ALLOWED")
    void increment_whenObserveTtlExpires_shouldResetCounterAndReturnAllowed() throws InterruptedException {
        // given
        tested = RedisRateLimitRepository.builder()
                .redisTemplate(redisTemplate)
                .script(script)
                .targetUrl(targetUrl)
                .keyTemplate("rate-limit")
                .maxAttemptCount(maxAttemptCount)
                .observeTime(1L)
                .lockTime(lockTime)
                .build();

        for (int i = 0; i < maxAttemptCount; i++) {
            tested.increment(ip);
        }

        Thread.sleep(1500);

        // when
        RateLimitStatus status = tested.increment(ip);

        // then
        assertThat(status).isEqualTo(RateLimitStatus.ALLOWED);
    }

    @Test
    @DisplayName("IT increment() when different IPs, should track counters independently")
    void increment_whenDifferentIps_shouldTrackCountersIndependently() {
        // given
        String anotherIp = "10.0.0.1";
        for (int i = 0; i < maxAttemptCount; i++) {
            tested.increment(ip);
        }
        tested.increment(ip);

        // when
        RateLimitStatus status = tested.increment(anotherIp);

        // then
        assertThat(status).isEqualTo(RateLimitStatus.ALLOWED);
    }
}