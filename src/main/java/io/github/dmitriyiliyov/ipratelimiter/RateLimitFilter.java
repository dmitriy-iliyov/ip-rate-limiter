package io.github.dmitriyiliyov.ipratelimiter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
public abstract class RateLimitFilter extends OncePerRequestFilter {

    private final ObjectMapper mapper;
    protected final Map<String, RateLimitRepository> repositoriesMap;

    public RateLimitFilter(ObjectMapper mapper, List<RateLimitRepository> repositories) {
        Objects.requireNonNull(mapper, "mapper cannot be null");
        Objects.requireNonNull(repositories, "repositories cannot be null");
        this.mapper = mapper;
        this.repositoriesMap = new HashMap<>();
        repositories.forEach(repository -> repositoriesMap.put(repository.getTargetUrl(), repository));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (repositoriesMap.containsKey(request.getRequestURI())) {
            RateLimitRepository repository = repositoriesMap.get(request.getRequestURI());
            String ip = extractIp(request);
            RateLimitStatus status = repository.increment(ip);
            if (status.equals(RateLimitStatus.BLOCKED)) {
                log.debug("IP {} is blocked", ip);
                handleBlocked(response);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    protected abstract String extractIp(HttpServletRequest request);

    protected void handleBlocked(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.getWriter().write(mapper.writeValueAsString(Map.of("message", "Too many requests, try later.")));
    }
}
