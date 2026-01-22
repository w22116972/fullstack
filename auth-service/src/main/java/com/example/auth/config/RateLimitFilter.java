package com.example.auth.config;

import com.example.auth.service.TokenCacheService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import org.springframework.core.annotation.Order;

import java.io.IOException;

/**
 * RateLimitFilter applies distributed rate limiting to the login endpoint using Redis.
 * <p>
 * This filter intercepts incoming HTTP requests and applies rate limiting to the /login endpoint.
 * It tracks the number of login attempts from each client IP address within a configurable time window
 * using Redis for distributed state. If the number of attempts exceeds the allowed limit, further 
 * requests from that IP are blocked with a 429 Too Many Requests response.
 * <p>
 * This implementation is distributed and works across multiple auth-service instances,
 * providing consistent rate limiting across the entire microservice cluster.
 */
@Slf4j
@Component
@Order(1)  // Ensure this filter runs before other filters
public class RateLimitFilter extends OncePerRequestFilter {

    private final TokenCacheService tokenCacheService;

    @Value("${rate.limit.login:5}")
    private int loginLimit;

    @Value("${rate.limit.login.window:60}")
    private int loginWindowSeconds;

    public RateLimitFilter(TokenCacheService tokenCacheService) {
        this.tokenCacheService = tokenCacheService;
    }

    /**
     * Filters incoming HTTP requests and applies rate limiting to the /login endpoint.
     * <p>
     * If the request is a POST to /login, the filter checks if the client IP has exceeded
     * the allowed number of login attempts within the configured time window. If the limit
     * is exceeded, the request is blocked with a 429 response. Otherwise, the request proceeds.
     *
     * @param request      the incoming HTTP request
     * @param response     the HTTP response
     * @param filterChain  the filter chain
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Only rate limit login endpoint
        if (path.contains("/login") && "POST".equalsIgnoreCase(request.getMethod())) {
            String clientIdentifier = getClientIdentifier(request);

            if (tokenCacheService.isRateLimitExceeded(clientIdentifier, loginLimit)) {
                log.warn("Rate limit exceeded for: {}", clientIdentifier);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Too many login attempts. Please try again later.\"}");
                return;
            }
            
            // Increment counter for this attempt
            tokenCacheService.incrementRateLimit(clientIdentifier, loginWindowSeconds, loginLimit);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Gets a client identifier for rate limiting.
     * Prefers email from request body if available, otherwise falls back to IP address.
     *
     * @param request the HTTP request
     * @return the client identifier
     */
    private String getClientIdentifier(HttpServletRequest request) {
        // For login, we could extract email from request body, but for simplicity use IP
        // In production, consider parsing JSON body to extract email
        return getClientIp(request);
    }

    /**
     * Extracts the client IP address from the HTTP request, considering common proxy headers.
     * Checks the X-Forwarded-For and X-Real-IP headers before falling back to the remote address.
     *
     * @param request the HTTP request
     * @return the client IP address as a String
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
