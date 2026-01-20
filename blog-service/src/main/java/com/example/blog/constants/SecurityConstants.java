package com.example.blog.constants;

/**
 * Security constants for the blog service.
 * 
 * Contains role names and error codes used in authorization checks.
 * 
 * Note: JWT-related constants are removed as JWT handling
 * is delegated to the auth-service/gateway layer.
 */
public final class SecurityConstants {
    private SecurityConstants() {}

    // Role names for @PreAuthorize checks
    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    // Error codes (kept for consistency)
    public static final String ERROR_RATE_LIMIT = "RATE_LIMIT_EXCEEDED";
    public static final String ERROR_ACCESS_DENIED = "ACCESS_DENIED";
}
