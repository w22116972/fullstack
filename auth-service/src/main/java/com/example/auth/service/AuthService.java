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
            
            log.info("User {} logged in successfully", loginRequest.getEmail());
            
            return new AuthResponse(
                token,
                user.getUsername(),
                user.getEmail(),
                user.getRole().toString()
            );
        } catch (BadCredentialsException e) {
            log.warn("Invalid credentials for user: {}", loginRequest.getEmail());
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
     * 
     * @param token the JWT token to validate
     * @return true if token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        return jwtUtil.validateToken(token);
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
}
