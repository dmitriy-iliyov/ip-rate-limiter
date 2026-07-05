package io.github.dmitriyiliyov.ipratelimiter;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@ConditionalOnProperty(
        prefix = "rate-limiter",
        name = "type",
        havingValue = "proxy"
)
@Component
public class ProxyRateLimitFilter extends RateLimitFilter {


    public ProxyRateLimitFilter(ObjectMapper mapper, List<RateLimitRepository> repositories) {
        super(mapper, repositories);
    }

    @Override
    protected String extractIp(HttpServletRequest request) {
        String xffHeader = request.getHeader("X-Forwarded-For");
        if (xffHeader == null || xffHeader.isBlank()) {
            return request.getRemoteAddr();
        } else {
            return xffHeader.split(",")[0].trim();
        }
    }
}
