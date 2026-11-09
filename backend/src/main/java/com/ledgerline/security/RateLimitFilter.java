package com.ledgerline.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final Map<String, Integer> LIMITS = Map.of(
            "/api/auth/login", 10,
            "/api/auth/register", 5,
            "/api/deposits", 30,
            "/api/transfers", 30);
    private static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final StringRedisTemplate redis;

    public RateLimitFilter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Integer limit = LIMITS.get(request.getRequestURI());
        if (limit == null || !MUTATING_METHODS.contains(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String key = "ledgerline:rate:" + request.getRequestURI() + ":" + hash(clientAddress(request));
        try {
            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1) {
                redis.expire(key, WINDOW.toSeconds(), TimeUnit.SECONDS);
            }
            response.setHeader("RateLimit-Limit", String.valueOf(limit));
            response.setHeader("RateLimit-Remaining", String.valueOf(Math.max(0, limit - (count == null ? 0 : count))));
            if (count != null && count > limit) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded\"}");
                return;
            }
        } catch (DataAccessException exception) {
            log.error("Redis rate limiting is unavailable for {}", request.getRequestURI(), exception);
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Rate limiting is temporarily unavailable");
            return;
        }
        chain.doFilter(request, response);
    }

    private String clientAddress(HttpServletRequest request) {
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
