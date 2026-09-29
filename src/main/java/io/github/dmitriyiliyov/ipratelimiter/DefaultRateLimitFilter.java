package io.github.dmitriyiliyov.ipratelimiter;

import jakarta.servlet.http.HttpServletRequest;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

public class DefaultRateLimitFilter extends RateLimitFilter {

    public DefaultRateLimitFilter(ObjectMapper mapper, List<RateLimitRepository> repositories) {
        super(mapper, repositories);
    }

    @Override
    protected String extractIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
