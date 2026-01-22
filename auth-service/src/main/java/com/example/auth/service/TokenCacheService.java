package com.example.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis-based token management service with graceful degradation.
 * <p>
 * Handles token blacklisting for logout, refresh token storage, and session management.
 * Uses Redis to provide distributed token state across multiple auth-service instances.
 * <p>
 * Supports lazy initialization - if Redis is not available, operations are logged
 * and execution continues. This allows auth-service to operate without Redis,
 * with reduced security (no distributed token blacklist across instances).
 */
@Slf4j
@Service
public class TokenCacheService {

    private final ObjectProvider<RedisTemplate<String, String>> redisTemplateProvider;
    private RedisTemplate<String, String> redisTemplate;
    private boolean redisInitialized = false;
    private boolean redisAvailable = false;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    // Redis key prefixes
    private static final String BLACKLIST_JTI_PREFIX = "blacklist_jti:";
    private static final String REFRESH_TOKEN_PREFIX = "refresh:";
    private static final String SESSION_PREFIX = "session:";

    /**
     * Constructor with lazy Redis template provider.
     * Uses ObjectProvider for true lazy initialization - Redis connection
     * is only attempted when first needed, not at startup.
     *
     * @param redisTemplateProvider lazy provider for RedisTemplate
     */
    public TokenCacheService(ObjectProvider<RedisTemplate<String, String>> redisTemplateProvider) {
        this.redisTemplateProvider = redisTemplateProvider;
        log.info("TokenCacheService initialized (Redis connection will be attempted lazily)");
    }

    /**
     * Lazily initializes Redis connection on first use.
     * This allows the service to start even if Redis is unavailable.
     */
    private synchronized void initRedisIfNeeded() {
        if (redisInitialized) {
            return;
        }
        redisInitialized = true;
        try {
            this.redisTemplate = redisTemplateProvider.getIfAvailable();
            if (this.redisTemplate != null) {
                // Test connection
                this.redisTemplate.getConnectionFactory().getConnection().ping();
                this.redisAvailable = true;
                log.info("Redis connection established successfully");
            } else {
                log.warn("RedisTemplate not available - token caching disabled");
            }
        } catch (Exception e) {
            log.warn("Failed to connect to Redis - token caching disabled: {}", e.getMessage());
            this.redisTemplate = null;
            this.redisAvailable = false;
        }
    }

    /**
     * Checks if Redis is available.
     *
     * @return true if Redis connection is available, false otherwise
     */
    private boolean isRedisAvailable() {
        initRedisIfNeeded();
        return redisAvailable && redisTemplate != null;
    }

    /**
     * Blacklists a JWT token by its JTI (JWT ID) claim for logout.
     * Token is automatically removed from Redis after its expiration time.
     * JTI-based blacklisting is more memory efficient than storing full tokens.
     * <p>
     * If Redis is unavailable, this operation is skipped with a warning.
     *
     * @param jti the JWT ID claim to blacklist
     */
    public void blacklistTokenByJti(String jti) {
        if (!isRedisAvailable()) {
            log.debug("Redis unavailable - skipping JTI blacklist for: {}", jti);
            return;
        }
        try {
            long ttl = jwtExpiration;
            String blacklistKey = BLACKLIST_JTI_PREFIX + jti;
            redisTemplate.opsForValue().set(blacklistKey, "revoked", ttl, TimeUnit.MILLISECONDS);
            log.debug("Token JTI blacklisted: {}", jti);
        } catch (Exception e) {
            log.warn("Failed to blacklist JTI: {}", e.getMessage());
        }
    }

    /**
     * Checks if a JTI has been blacklisted.
     * <p>
     * If Redis is unavailable, returns false (conservative approach - allows token).
     *
     * @param jti the JWT ID to check
     * @return true if JTI is blacklisted, false otherwise (or if Redis unavailable)
     */
    public boolean isJtiBlacklisted(String jti) {
        if (!isRedisAvailable()) {
            log.debug("Redis unavailable - allowing JTI (not checking blacklist): {}", jti);
            return false;
        }
        try {
            String blacklistKey = BLACKLIST_JTI_PREFIX + jti;
            return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));
        } catch (Exception e) {
            log.warn("Failed to check JTI blacklist: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Stores a refresh token for token refresh operations.
     * Refresh tokens typically have longer expiration (e.g., 7 days).
     * <p>
     * If Redis is unavailable, this operation is skipped with a warning.
     *
     * @param username the username
     * @param refreshToken the refresh token
     * @param expirationMs the expiration time in milliseconds
     */
    public void storeRefreshToken(String username, String refreshToken, long expirationMs) {
        if (!isRedisAvailable()) {
            log.debug("Redis unavailable - skipping refresh token storage for user: {}", username);
            return;
        }
        try {
            String key = REFRESH_TOKEN_PREFIX + username;
            redisTemplate.opsForValue().set(key, refreshToken, expirationMs, TimeUnit.MILLISECONDS);
            log.debug("Refresh token stored for user: {}", username);
        } catch (Exception e) {
            log.warn("Failed to store refresh token: {}", e.getMessage());
        }
    }

    /**
     * Retrieves a stored refresh token.
     * <p>
     * If Redis is unavailable, returns null.
     *
     * @param username the username
     * @return the refresh token if exists, null otherwise
     */
    public String getRefreshToken(String username) {
        if (!isRedisAvailable()) {
            log.debug("Redis unavailable - cannot retrieve refresh token for user: {}", username);
            return null;
        }
        try {
            String key = REFRESH_TOKEN_PREFIX + username;
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Failed to retrieve refresh token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Revokes a refresh token (on logout).
     * <p>
     * If Redis is unavailable, this operation is skipped with a warning.
     *
     * @param username the username
     */
    public void revokeRefreshToken(String username) {
        if (!isRedisAvailable()) {
            log.debug("Redis unavailable - skipping refresh token revocation for user: {}", username);
            return;
        }
        try {
            String key = REFRESH_TOKEN_PREFIX + username;
            Boolean deleted = redisTemplate.delete(key);
            if (Boolean.TRUE.equals(deleted)) {
                log.debug("Refresh token revoked for user: {}", username);
            }
        } catch (Exception e) {
            log.warn("Failed to revoke refresh token: {}", e.getMessage());
        }
    }

    /**
     * Creates a user session entry.
     * Used to track active user sessions across the system.
     * <p>
     * If Redis is unavailable, this operation is skipped with a warning.
     *
     * @param username the username
     * @param sessionData JSON or simple session data
     * @param expirationMs the session expiration time in milliseconds
     */
    public void createSession(String username, String sessionData, long expirationMs) {
        if (!isRedisAvailable()) {
            log.debug("Redis unavailable - skipping session creation for user: {}", username);
            return;
        }
        try {
            String key = SESSION_PREFIX + username;
            redisTemplate.opsForValue().set(key, sessionData, expirationMs, TimeUnit.MILLISECONDS);
            log.debug("Session created for user: {}", username);
        } catch (Exception e) {
            log.warn("Failed to create session: {}", e.getMessage());
        }
    }

    /**
     * Gets an active user session.
     * <p>
     * If Redis is unavailable, returns null.
     *
     * @param username the username
     * @return session data if exists, null otherwise
     */
    public String getSession(String username) {
        if (!isRedisAvailable()) {
            log.debug("Redis unavailable - cannot retrieve session for user: {}", username);
            return null;
        }
        try {
            String key = SESSION_PREFIX + username;
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Failed to retrieve session: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Invalidates a user session.
     * <p>
     * If Redis is unavailable, this operation is skipped with a warning.
     *
     * @param username the username
     */
    public void invalidateSession(String username) {
        if (!isRedisAvailable()) {
            log.debug("Redis unavailable - skipping session invalidation for user: {}", username);
            return;
        }
        try {
            String key = SESSION_PREFIX + username;
            Boolean deleted = redisTemplate.delete(key);
            if (Boolean.TRUE.equals(deleted)) {
                log.debug("Session invalidated for user: {}", username);
            }
        } catch (Exception e) {
            log.warn("Failed to invalidate session: {}", e.getMessage());
        }
    }

    /**
     * Checks if a user has an active session.
     * <p>
     * If Redis is unavailable, returns false.
     *
     * @param username the username
     * @return true if session exists, false otherwise
     */
    public boolean hasActiveSession(String username) {
        if (!isRedisAvailable()) {
            log.debug("Redis unavailable - cannot check session for user: {}", username);
            return false;
        }
        try {
            String key = SESSION_PREFIX + username;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.warn("Failed to check session: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Increments a rate limit counter for an identifier (e.g., email or IP).
     * Used for login attempt throttling.
     * <p>
     * If Redis is unavailable, returns 0 and allows the operation (no rate limiting).
     *
     * @param identifier the identifier (email, IP, etc.)
     * @param windowSeconds the rate limit window in seconds
     * @param maxAttempts the maximum allowed attempts
     * @return current attempt count
     */
    public long incrementRateLimit(String identifier, long windowSeconds, long maxAttempts) {
        if (!isRedisAvailable()) {
            log.debug("Redis unavailable - skipping rate limit increment for: {}", identifier);
            return 0;
        }
        try {
            String key = "ratelimit:" + identifier;
            Long count = redisTemplate.opsForValue().increment(key);
            
            // Set expiration on first increment
            if (count != null && count == 1) {
                redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
            }
            
            return count != null ? count : 0;
        } catch (Exception e) {
            log.warn("Failed to increment rate limit: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Checks if rate limit has been exceeded.
     * <p>
     * If Redis is unavailable, returns false (allows the operation).
     *
     * @param identifier the identifier
     * @param maxAttempts the maximum allowed attempts
     * @return true if limit exceeded, false otherwise
     */
    public boolean isRateLimitExceeded(String identifier, long maxAttempts) {
        if (!isRedisAvailable()) {
            log.debug("Redis unavailable - skipping rate limit check for: {}", identifier);
            return false;
        }
        try {
            String key = "ratelimit:" + identifier;
            String count = redisTemplate.opsForValue().get(key);
            if (count == null) {
                return false;
            }
            return Long.parseLong(count) >= maxAttempts;
        } catch (Exception e) {
            log.warn("Failed to check rate limit: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Resets rate limit counter.
     * <p>
     * If Redis is unavailable, this operation is skipped with a warning.
     *
     * @param identifier the identifier
     */
    public void resetRateLimit(String identifier) {
        if (!isRedisAvailable()) {
            log.debug("Redis unavailable - skipping rate limit reset for: {}", identifier);
            return;
        }
        try {
            String key = "ratelimit:" + identifier;
            redisTemplate.delete(key);
            log.debug("Rate limit reset for: {}", identifier);
        } catch (Exception e) {
            log.warn("Failed to reset rate limit: {}", e.getMessage());
        }
    }
}
