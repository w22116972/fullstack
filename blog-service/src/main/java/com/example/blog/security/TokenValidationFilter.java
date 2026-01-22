package com.example.blog.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Arrays;

/**
 * Security filter for blog-service to validate JWT tokens and check JTI blacklist.
 * <p>
 * This filter intercepts all requests and validates JWT tokens against the auth-cache
 * (shared Redis) to ensure logged-out tokens are rejected even if their signature is valid.
 * <p>
 * Uses lazy Redis initialization - if Redis is unavailable, the filter allows requests
 * to proceed (fail-open behavior) while logging a warning.
 *
 * Checks both:
 * 1. Token signature and expiration (delegated to JwtUtil)
 * 2. Token JTI blacklist status (from auth-cache Redis)
 */
@Slf4j
@Component
public class TokenValidationFilter extends OncePerRequestFilter {

    private final RedisTemplate<String, String> redisTemplate;
    private boolean redisAvailable = false;
    private boolean redisInitialized = false;
    private static final String BLACKLIST_JTI_PREFIX = "blacklist_jti:";

    public TokenValidationFilter(@Qualifier("redisTemplate") RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Lazily initializes Redis connection on first use.
     */
    private synchronized void initRedisIfNeeded() {
        if (redisInitialized) {
            return;
        }
        redisInitialized = true;
        try {
            if (this.redisTemplate != null) {
                this.redisTemplate.getConnectionFactory().getConnection().ping();
                this.redisAvailable = true;
                log.info("TokenValidationFilter: Redis connection established");
            } else {
                log.warn("TokenValidationFilter: Redis not available - JTI blacklist checking disabled");
            }
        } catch (Exception e) {
            log.warn("TokenValidationFilter: Failed to connect to Redis - JTI blacklist checking disabled: {}", e.getMessage());
            this.redisAvailable = false;
        }
    }

    /**
     * Extracts JWT token from request (Authorization header or cookie).
     *
     * @param request the HTTP request
     * @return the JWT token if present, null otherwise
     */
    private String extractToken(HttpServletRequest request) {
        // Try Authorization header first
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // Try cookie (matches JwtAuthFilter behavior)
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if ("token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * Extracts JWT ID (JTI) claim from token.
     * JTI format is typically a UUID string (e.g., "550e8400-e29b-41d4-a716-446655440000")
     *
     * @param token the JWT token
     * @return the JTI claim value, or null if not found
     */
    private String extractJtiFromToken(String token) {
        try {
            // Split token into parts: header.payload.signature
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            
            // Decode payload (base64)
            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
            
            // Extract JTI using simple string search (no JSON parsing needed)
            // Looking for: "jti":"uuid-value"
            int jtiIndex = payload.indexOf("\"jti\"");
            if (jtiIndex == -1) {
                return null;
            }
            
            int startIndex = payload.indexOf("\"", jtiIndex + 5) + 1;
            int endIndex = payload.indexOf("\"", startIndex);
            
            if (startIndex > 0 && endIndex > startIndex) {
                return payload.substring(startIndex, endIndex);
            }
        } catch (Exception e) {
            log.warn("Failed to extract JTI from token: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Checks if a JTI has been blacklisted in Redis.
     *
     * @param jti the JWT ID to check
     * @return true if JTI is blacklisted, false otherwise (also returns false if Redis unavailable)
     */
    private boolean isJtiBlacklisted(String jti) {
        initRedisIfNeeded();
        if (!redisAvailable || redisTemplate == null) {
            log.debug("Redis unavailable - skipping JTI blacklist check");
            return false;
        }
        try {
            String blacklistKey = BLACKLIST_JTI_PREFIX + jti;
            return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));
        } catch (Exception e) {
            log.warn("Failed to check JTI blacklist: {}", e.getMessage());
            // If Redis is down, allow request to proceed (fail open)
            return false;
        }
    }

    /**
     * Determines if the current request should skip token validation.
     * Public endpoints don't require authentication.
     *
     * @param request the HTTP request
     * @return true if validation should be skipped
     */
    private boolean shouldSkipValidation(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Public endpoints that don't require authentication
        String[] publicPaths = {
            "/actuator",
            "/health",
            "/swagger-ui",
            "/v3/api-docs",
            "/api/public"
        };
        
        return Arrays.stream(publicPaths)
            .anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // Skip validation for public endpoints
        if (shouldSkipValidation(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            // Extract token from Authorization header or cookie
            String token = extractToken(request);
            log.info("TokenValidationFilter: {} {} - token present: {}", request.getMethod(), request.getRequestURI(), token != null);

            // If no token provided, allow request (SecurityFilterChain will handle auth)
            if (token == null) {
                log.info("TokenValidationFilter: No token, continuing filter chain");
                filterChain.doFilter(request, response);
                return;
            }

            // Extract JTI from token
            String jti = extractJtiFromToken(token);
            log.info("TokenValidationFilter: JTI extracted: {}", jti != null ? jti.substring(0, Math.min(8, jti.length())) + "..." : "null");

            // If JTI not found or blacklisted, reject request
            if (jti == null) {
                log.warn("TokenValidationFilter: Token missing JTI claim");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\": \"Invalid token: missing JTI\"}");
                return;
            }

            if (isJtiBlacklisted(jti)) {
                log.warn("TokenValidationFilter: Token JTI is blacklisted: {}", jti);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\": \"Token has been revoked\"}");
                return;
            }

            log.info("TokenValidationFilter: Token valid, continuing filter chain");
            // Token is valid, proceed with request
            filterChain.doFilter(request, response);
            
        } catch (Exception e) {
            log.error("Error during token validation: {}", e.getMessage());
            // If unexpected error occurs, reject request for security
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Token validation failed\"}");
        }
    }
}
