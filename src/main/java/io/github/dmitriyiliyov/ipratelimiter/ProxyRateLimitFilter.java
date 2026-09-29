package io.github.dmitriyiliyov.ipratelimiter;

import jakarta.servlet.http.HttpServletRequest;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

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
