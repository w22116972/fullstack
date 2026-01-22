package com.example.auth.service;

import com.example.auth.dto.AuthResponse;
import com.example.auth.dto.LoginRequest;
import com.example.auth.dto.RegisterRequest;
import com.example.auth.exception.AuthException;
import com.example.auth.model.Role;
import com.example.auth.model.User;
import com.example.auth.repository.UserRepository;
import com.example.auth.security.JwtUtil;
import com.example.auth.validation.PasswordValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service for authentication operations.
 * Handles user login, registration, and JWT token generation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final PasswordValidator passwordValidator;
    private final TokenCacheService tokenCacheService;
    
    private static final long REFRESH_TOKEN_EXPIRATION = 7 * 24 * 60 * 60 * 1000; // 7 days

    /**
     * Authenticates a user and generates a JWT token.
     * 
     * @param loginRequest the login credentials
     * @return AuthResponse containing JWT token and user details
     * @throws AuthException if authentication fails
     */
    public AuthResponse login(LoginRequest loginRequest) {
        log.info("Attempting login for user: {}", loginRequest.getEmail());
        
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getEmail(),
                    loginRequest.getPassword()
                )
            );

            User user = (User) authentication.getPrincipal();
            String token = jwtUtil.generateToken(user);
            
            // Create user session in cache
            tokenCacheService.createSession(
                user.getUsername(),
                user.getEmail(),
                60 * 60 * 1000 // 1 hour session
            );
            
            // Store refresh token in Redis for token refresh operations
            String refreshToken = generateRefreshToken(user);
            tokenCacheService.storeRefreshToken(
                user.getUsername(),
                refreshToken,
                REFRESH_TOKEN_EXPIRATION
            );
            
            log.info("User {} logged in successfully", loginRequest.getEmail());
            
            return new AuthResponse(
                token,
                user.getUsername(),
                user.getEmail(),
                user.getRole().toString()
            );
        } catch (BadCredentialsException e) {
            log.warn("Invalid credentials for user: {}", loginRequest.getEmail());
            // Increment rate limit for failed attempt
            tokenCacheService.incrementRateLimit(
                loginRequest.getEmail(),
                60, // 60 second window
                5   // max 5 attempts
            );
            throw new AuthException("Invalid email or password", e);
        }
    }

    /**
     * Registers a new user account.
     * 
     * @param registerRequest the registration details
     * @return AuthResponse containing JWT token for newly registered user
     * @throws AuthException if registration fails
     */
    public AuthResponse register(RegisterRequest registerRequest) {
        log.info("Attempting registration for email: {}", registerRequest.getEmail());
        
        // Validate password strength
        if (!passwordValidator.isValid(registerRequest.getPassword())) {
            throw new AuthException(passwordValidator.getValidationMessage());
        }
        
        // Check if email already exists
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            log.warn("Registration failed: email already exists - {}", registerRequest.getEmail());
            throw new AuthException("Email already registered");
        }
        
        // Create new user
        User user = new User();
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setRole(Role.USER);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        // Save user to database
        User savedUser = userRepository.save(user);
        
        // Generate JWT token
        String token = jwtUtil.generateToken(savedUser);
        
        log.info("User {} registered successfully", registerRequest.getEmail());
        
        return new AuthResponse(
            token,
            savedUser.getUsername(),
            savedUser.getEmail(),
            savedUser.getRole().toString()
        );
    }

    /**
     * Validates a JWT token.
     * Checks both signature/expiration and JTI blacklist status.
     * 
     * @param token the JWT token to validate
     * @return true if token is valid and not blacklisted, false otherwise
     */
    public boolean validateToken(String token) {
        // Verify signature and expiration
        if (!jwtUtil.validateToken(token)) {
            return false;
        }
        
        // Check if JTI is blacklisted
        String jti = jwtUtil.extractJti(token);
        if (jti != null && tokenCacheService.isJtiBlacklisted(jti)) {
            return false;
        }
        
        return true;
    }

    /**
     * Extracts username from a JWT token.
     * 
     * @param token the JWT token
     * @return the username/email extracted from token
     */
    public String extractUsername(String token) {
        return jwtUtil.extractUsername(token);
    }

    /**
     * Revokes a JWT token by blacklisting its JTI claim.
     * Also invalidates user session and refresh token.
     * JTI-based blacklisting is more memory efficient than full token storage.
     * 
     * @param token the JWT token to revoke
     * @param username the username to revoke session/refresh token for
     */
    public void logout(String token, String username) {
        if (token != null && !token.isEmpty()) {
            // Extract JTI and blacklist it (memory efficient approach)
            String jti = jwtUtil.extractJti(token);
            if (jti != null) {
                tokenCacheService.blacklistTokenByJti(jti);
            }
            
            // Invalidate user session
            tokenCacheService.invalidateSession(username);
            
            // Revoke refresh token
            tokenCacheService.revokeRefreshToken(username);
            
            log.info("User {} logged out successfully. JTI {} blacklisted, session invalidated.", username, jti);
        }
    }

    /**
     * Generates a refresh token (can be a separate JWT or a random token).
     * For simplicity, using a simple random token; in production, consider using JTI claim.
     * 
     * @param user the user
     * @return a refresh token string
     */
    private String generateRefreshToken(User user) {
        // In production, consider using a JTI (JWT ID) claim and storing it in Redis
        return java.util.UUID.randomUUID().toString();
    }

    /**
     * Validates rate limit for login attempts.
     * 
     * @param email the user email
     * @return true if rate limit is exceeded, false otherwise
     */
    public boolean isLoginRateLimited(String email) {
        return tokenCacheService.isRateLimitExceeded(email, 5);
    }

    /**
     * Resets rate limit for successful login.
     * 
     * @param email the user email
     */
    public void resetLoginRateLimit(String email) {
        tokenCacheService.resetRateLimit(email);
    }

    /**
     * Refreshes an access token using a valid refresh token.
     * 
     * @param refreshToken the refresh token (UUID string stored in Redis)
     * @return AuthResponse with new JWT access token if valid, or error response
     * @throws AuthException if refresh token is invalid or revoked
     */
    public AuthResponse refreshAccessToken(String refreshToken) {
        try {
            // Step 1: Verify refresh token is not empty
            if (refreshToken == null || refreshToken.isEmpty()) {
                throw new AuthException("Refresh token is required");
            }
            
            // Step 2: Look up the user associated with this refresh token
            // The refresh token is stored as: refresh:{username} -> {refreshToken}
            // We need to find which user this token belongs to
            // For now, we'll need the username from the refresh token mapping
            // In a production system, consider storing: refresh_by_token:{token} -> {username}
            
            log.warn("Refresh token lookup requires enhanced Redis mapping. " +
                    "Consider storing reverse mapping: refresh_by_token -> username");
            throw new AuthException("Refresh token refresh not fully implemented. " +
                    "Endpoint needs reverse token mapping in Redis.");
            
        } catch (AuthException e) {
            log.warn("Token refresh failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during token refresh: {}", e.getMessage());
            throw new AuthException("Failed to refresh token", e);
        }
    }
    
    /**
     * Refreshes an access token using a valid refresh token and username.
     * This is an enhanced version that takes username for token lookup.
     * 
     * @param username the username/email of the user
     * @param refreshToken the refresh token
     * @return AuthResponse with new JWT access token
     * @throws AuthException if refresh token is invalid or revoked
     */
    public AuthResponse refreshAccessToken(String username, String refreshToken) {
        try {
            // Step 1: Verify inputs
            if (username == null || username.isEmpty()) {
                throw new AuthException("Username is required");
            }
            if (refreshToken == null || refreshToken.isEmpty()) {
                throw new AuthException("Refresh token is required");
            }
            
            // Step 2: Retrieve stored refresh token from Redis
            String storedRefreshToken = tokenCacheService.getRefreshToken(username);
            if (storedRefreshToken == null || storedRefreshToken.isEmpty()) {
                throw new AuthException("Refresh token not found or has expired");
            }
            
            // Step 3: Compare provided token with stored token
            if (!storedRefreshToken.equals(refreshToken)) {
                log.warn("Refresh token mismatch for user: {}", username);
                throw new AuthException("Invalid refresh token");
            }
            
            // Step 4: Load user from database
            User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new AuthException("User not found: " + username));
            
            // Step 5: Generate new access token with new JTI and expiration
            String newAccessToken = jwtUtil.generateToken(user);
            
            // Step 6: Optionally, generate a new refresh token (token rotation)
            // This is more secure but requires updating Redis
            String newRefreshToken = generateRefreshToken(user);
            tokenCacheService.storeRefreshToken(
                username,
                newRefreshToken,
                REFRESH_TOKEN_EXPIRATION
            );
            
            log.info("Access token refreshed successfully for user: {}", username);
            
            return new AuthResponse(
                newAccessToken,
                user.getUsername(),
                user.getEmail(),
                user.getRole().toString()
            );
            
        } catch (AuthException e) {
            log.warn("Token refresh failed for user {}: {}", username, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during token refresh for user {}: {}", username, e.getMessage());
            throw new AuthException("Failed to refresh token", e);
        }
    }
}
