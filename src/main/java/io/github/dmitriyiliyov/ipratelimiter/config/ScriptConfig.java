package io.github.dmitriyiliyov.ipratelimiter.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

@Configuration
public class ScriptConfig {

    @Bean
    public DefaultRedisScript<Boolean> rateLimitScript(@Value("${rate-limiter.script-path}") String scriptPath) {
        DefaultRedisScript<Boolean> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource(scriptPath)));
        script.setResultType(Boolean.class);
        return script;
    }
}
