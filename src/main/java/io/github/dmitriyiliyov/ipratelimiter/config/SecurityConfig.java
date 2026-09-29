package io.github.dmitriyiliyov.ipratelimiter.config;

import io.github.dmitriyiliyov.ipratelimiter.DefaultRateLimitFilter;
import io.github.dmitriyiliyov.ipratelimiter.ProxyRateLimitFilter;
import io.github.dmitriyiliyov.ipratelimiter.RateLimitFilter;
import io.github.dmitriyiliyov.ipratelimiter.RateLimitRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@EnableWebSecurity
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   ObjectMapper mapper,
                                                   List<RateLimitRepository> repositories,
                                                   @Value("${rate-limiter.type:default}") String type) {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .addFilterBefore(rateLimitFilter(type, mapper, repositories), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }

    private RateLimitFilter rateLimitFilter(String type, ObjectMapper mapper, List<RateLimitRepository> repositories) {
        return switch (type) {
            case "default" -> new DefaultRateLimitFilter(mapper, repositories);
            case "proxy" -> new ProxyRateLimitFilter(mapper, repositories);
            default -> throw new IllegalArgumentException("Unknown rate-limiter.type: " + type);
        };
    }
}
