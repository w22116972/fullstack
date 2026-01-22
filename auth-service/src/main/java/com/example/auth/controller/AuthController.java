package com.example.auth.controller;

import com.example.auth.dto.AuthResponse;
import com.example.auth.dto.LoginRequest;
import com.example.auth.dto.RegisterRequest;
import com.example.auth.dto.TokenValidationRequest;
import com.example.auth.dto.ErrorResponse;
import com.example.auth.dto.TokenValidationResponse;
import com.example.auth.dto.RefreshTokenRequest;
import com.example.auth.exception.AuthException;
import com.example.auth.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AuthController exposes REST endpoints for authentication operations.
 * <p>
 * Handles user login, registration, logout, and token validation. Integrates with AuthService
 * for business logic, manages JWT token cookies, and provides error responses for failed operations.
 * CORS is configured to allow requests from the frontend.
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "${cors.allowed-origins:http://localhost:8080}", allowCredentials = "true")
public class AuthController {

    private final AuthService authService;

    @Value("${jwt.expiration:36000000}")
    private long jwtExpiration;

    /**
     * Purpose: Authenticate a user and establish a session using JWT.
     * Handles user login requests.
     * <p>
     * - Validates login credentials and delegates authentication to AuthService.
     * - On success, sets a JWT token cookie and returns user info.
     * - On failure, returns 401 Unauthorized with error details.
     *
     * @param loginRequest the login credentials
     * @param response the HTTP response (for setting cookies)
     * @return ResponseEntity with AuthResponse or error
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        try {
            AuthResponse authResponse = authService.login(loginRequest);
            setTokenCookie(response, authResponse.getToken());
            return ResponseEntity.ok(authResponse);
        } catch (AuthException e) {
            log.error("Login failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Purpose: Register a new user and immediately authenticate them with JWT.
     * Handles user registration requests.
     * <p>
     * - Validates registration data and delegates user creation to AuthService.
     * - On success, sets a JWT token cookie and returns user info.
     * - On failure, returns 400 Bad Request with error details.
     *
     * @param registerRequest the registration data
     * @param response the HTTP response (for setting cookies)
     * @return ResponseEntity with AuthResponse or error
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest, HttpServletResponse response) {
        try {
            AuthResponse authResponse = authService.register(registerRequest);
            setTokenCookie(response, authResponse.getToken());
            return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
        } catch (AuthException e) {
            log.error("Registration failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Purpose: Log out the user by clearing the authentication cookie and revoking the token.
     * Handles user logout requests.
     * <p>
     * - Extracts and blacklists the JWT token from the request cookie.
     * - Invalidates user session and refresh token.
     * - Clears the JWT token cookie by setting it to an empty value and maxAge=0.
     * - Returns 200 OK on success.
     *
     * @param request the HTTP request (for extracting token)
     * @param response the HTTP response (for clearing cookies)
     * @return ResponseEntity indicating success
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(jakarta.servlet.http.HttpServletRequest request, HttpServletResponse response) {
        try {
            // Extract token from cookie
            String token = null;
            String username = null;
            if (request.getCookies() != null) {
                for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                    if ("token".equals(cookie.getName())) {
                        token = cookie.getValue();
                        break;
                    }
                }
            }
            
            // Blacklist the token and invalidate session on server side
            if (token != null && !token.isEmpty()) {
                username = authService.extractUsername(token);
                authService.logout(token, username);
            }
            
            // Clear the token cookie
            Cookie cookie = new Cookie("token", "");
            cookie.setHttpOnly(true);
            cookie.setSecure(false); // Set to true in production with HTTPS
            cookie.setPath("/");
            cookie.setMaxAge(0);
            response.addCookie(cookie);
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Logout failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Logout failed"));
        }
    }

    /**
     * Purpose: Check if a JWT token is valid and extract user information.
     * Validates a JWT token.
     * <p>
     * - Delegates token validation to AuthService.
     * - On success, returns the username associated with the token.
     * - On failure, returns 401 Unauthorized.
     *
     * @param token the token validation request
     * @return ResponseEntity with validation result and username if valid
     */
    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestBody TokenValidationRequest token) {
        boolean isValid = authService.validateToken(token.getToken());
        if (isValid) {
            String username = authService.extractUsername(token.getToken());
            return ResponseEntity.ok(new TokenValidationResponse(true, username));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new TokenValidationResponse(false, null));
        }
    }

    /**
     * Purpose: Store the JWT token securely in the user's browser as an HTTP-only cookie.
     * Sets the JWT token as an HTTP-only cookie in the response.
     * <p>
     * - Used after successful login or registration.
     * - Cookie is not secure in development; set to true in production.
     *
     * @param response the HTTP response
     * @param token the JWT token to set
     */
    private void setTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Set to true in production with HTTPS
        cookie.setPath("/");
        cookie.setMaxAge((int) (jwtExpiration / 1000));
        response.addCookie(cookie);
    }

    /**
     * Purpose: Refresh an expired or expiring JWT token using a refresh token.
     * Handles token refresh requests for seamless session continuation.
     * <p>
     * - Validates the refresh token stored in Redis.
     * - Generates a new JWT token if refresh token is valid.
     * - Updates the cookie with the new token.
     * - Returns 200 OK with new token or 401 Unauthorized.
     *
     * @param refreshTokenRequest the refresh token request (containing username and refreshToken)
     * @param response the HTTP response (for setting new cookie)
     * @return ResponseEntity with new AuthResponse or error
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest, HttpServletResponse response) {
        try {
            String username = refreshTokenRequest.getUsername();
            String refreshToken = refreshTokenRequest.getRefreshToken();
            
            if (username == null || username.isEmpty() || refreshToken == null || refreshToken.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Username and refresh token are required"));
            }
            
            AuthResponse authResponse = authService.refreshAccessToken(username, refreshToken);
            setTokenCookie(response, authResponse.getToken());
            return ResponseEntity.ok(authResponse);
            
        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("Token refresh failed: " + e.getMessage()));
        }
    }

}
