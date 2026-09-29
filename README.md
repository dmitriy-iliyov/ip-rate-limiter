[![codecov](https://codecov.io/gh/dmitriy-iliyov/ip-rate-limiter/graph/badge.svg?token=9YJ5LQ45XF)](https://codecov.io/gh/dmitriy-iliyov/ip-rate-limiter)
[![CI](https://github.com/dmitriy-iliyov/ip-rate-limiter/actions/workflows/ci.yml/badge.svg)](https://github.com/dmitriy-iliyov/ip-rate-limiter/actions/workflows/ci.yml)

## Overview
A simple Spring Security IP-based request rate limiting filter using Redis and Lua scripting for atomic counter operations. 

Each incoming request to a configured endpoint is checked against a Redis counter for the client's IP.  
The counter increments atomically via a Lua script — if the count exceeds the limit within the observe window, the IP is blocked for a configurable duration and all further requests immediately receive `429 Too Many Requests`.

## Quick Start
Create your own rate limit repository:
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
- **proxy** - extracts the client IP address from the `X-Forwarded-For` header, making it suitable for applications deployed behind a reverse proxy or load balancer.

## Run
The application requires a running Redis instance. Connection is configured with `SPRING_DATA_REDIS_HOST` and `SPRING_DATA_REDIS_PORT` (defaults: `host.docker.internal:6379`).

```bash
  docker run -d -p 6379:6379 redis
```

```bash
  mvn clean package
```

```bash
  SPRING_DATA_REDIS_HOST=localhost java -jar target/ip-rate-limiter-1.0.0.jar
```

### Docker Compose
`docker-compose.yaml` reads `REDIS_HOST` and `REDIS_PORT` from a `.env` file, which is not committed. Create it before starting:

```bash
  printf "REDIS_HOST=rate-limiter-redis\nREDIS_PORT=6379\n" > .env
  docker compose up --build
```

## Limitations
- **Redis Cluster is not supported.** The Lua script uses two keys (`<keyTemplate>:ip:<ip>` and `blocked:<keyTemplate>:ip:<ip>`) that can hash to different slots, and Redis Cluster rejects the script with `CROSSSLOT`.
- **Fail-closed on Redis errors.** If Redis is unavailable, requests to rate-limited endpoints fail with `500`. Requests to other endpoints are not affected.
- **One repository per URL.** Registering two repositories with the same `targetUrl` fails at startup.
