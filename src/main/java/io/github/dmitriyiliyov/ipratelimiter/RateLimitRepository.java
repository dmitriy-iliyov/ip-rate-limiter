package io.github.dmitriyiliyov.ipratelimiter;

public interface RateLimitRepository {
    String getTargetUrl();
    RateLimitStatus increment(String remoteAddr);
}
