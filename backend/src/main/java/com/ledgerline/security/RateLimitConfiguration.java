package com.ledgerline.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RateLimitConfiguration {

    @Bean
    @ConditionalOnProperty(name = "ledgerline.redis.enabled", havingValue = "true")
    RateLimitFilter rateLimitFilter(StringRedisTemplate redis) {
        return new RateLimitFilter(redis);
    }
}
