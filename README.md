[![codecov](https://codecov.io/gh/dmitriy-iliyov/ip-rate-limiter/graph/badge.svg?token=9YJ5LQ45XF)](https://codecov.io/gh/dmitriy-iliyov/ip-rate-limiter)
[![CI](https://github.com/dmitriy-iliyov/ip-rate-limiter/actions/workflows/ci.yml/badge.svg)](https://github.com/dmitriy-iliyov/ip-rate-limiter/actions/workflows/ci.yml)

## Overview
A simple Spring Security IP-based request rate limiting filter using Redis and Lua scripting for atomic counter operations. 

Each incoming request to a configured endpoint is checked against a Redis counter for the client's IP.  
The counter increments atomically via a Lua script — if the count exceeds the limit within the observe window, the IP is blocked for a configurable duration and all further requests immediately receive `429 Too Many Requests`.

## Quick Start
Create your oun rate limit repository:
```java
    @Bean
    public RateLimitRepository rateLimitRepository(RedisTemplate<String, Boolean> redisTemplate,
                                                   DefaultRedisScript<Boolean> script) {
        return RedisRateLimitRepository.builder()
                .redisTemplate(redisTemplate)
                .script(script)
                .targetUrl("targetUrl")
                .keyTemplate("redis-key-template")
                .observeTime(Duration.ofSeconds(10).getSeconds())
                .lockTime(Duration.ofMinutes(15).getSeconds())
                .maxAttemptCount(3L)
                .build();
    }
```

A RateLimitRepository stores and manages rate-limiting state. You can configure multiple repository instances to protect different endpoints with different rate-limiting policies.

Two rate limiting filters are available:
- **default** - extracts the client IP address directly from the incoming request.
- **proxy** - extracts the client IP address from the `X-Forwarded-From` header, making it suitable for applications deployed behind a reverse proxy or load balancer.

## Run

```bash
  mvn clean package
```

```bash
  java -jar target/ip-rate-limiter-1.0.0.jar
```
