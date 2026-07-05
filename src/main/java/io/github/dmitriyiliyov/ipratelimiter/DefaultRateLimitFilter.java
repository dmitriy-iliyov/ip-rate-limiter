package io.github.dmitriyiliyov.ipratelimiter;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@ConditionalOnProperty(
        prefix = "rate-limiter",
        name = "type",
        havingValue = "default",
        matchIfMissing = true
)
@Component
public class DefaultRateLimitFilter extends RateLimitFilter {

    public DefaultRateLimitFilter(ObjectMapper mapper, List<RateLimitRepository> repositories) {
        super(mapper, repositories);
    }

    @Override
    protected String extractIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
