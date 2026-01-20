package com.example.auth.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;

import java.security.Key;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private UserDetails testUser;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // Use reflection to set the secret and expiration
        try {
            var secretField = JwtUtil.class.getDeclaredField("secret");
            secretField.setAccessible(true);
            secretField.set(jwtUtil, "dGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQ=");

            var expirationField = JwtUtil.class.getDeclaredField("expiration");
            expirationField.setAccessible(true);
            expirationField.set(jwtUtil, 3600000L); // 1 hour
        } catch (Exception e) {
            fail("Failed to initialize JwtUtil", e);
        }

        testUser = User.builder()
            .username("test@example.com")
            .password("password")
            .roles("USER")
            .build();
    }

    @Test
    void testGenerateTokenSuccess() {
        // Act
        String token = jwtUtil.generateToken(testUser);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3); // JWT has 3 parts
    }

    @Test
    void testGenerateTokenWithExtraClaims() {
        // Arrange
        Map<String, Object> extraClaims = new java.util.HashMap<>();
        extraClaims.put("role", "ADMIN");
        extraClaims.put("userId", "123");

        // Act
        String token = jwtUtil.generateToken(extraClaims, testUser);

        // Assert
        assertNotNull(token);
        String username = jwtUtil.extractUsername(token);
        assertEquals("test@example.com", username);
    }

    @Test
    void testExtractUsernameSuccess() {
        // Arrange
        String token = jwtUtil.generateToken(testUser);

        // Act
        String username = jwtUtil.extractUsername(token);

        // Assert
        assertEquals("test@example.com", username);
    }

    @Test
    void testExtractClaimSuccess() {
        // Arrange
        String token = jwtUtil.generateToken(testUser);

        // Act
        Date issuedAt = jwtUtil.extractClaim(token, Claims::getIssuedAt);

        // Assert
        assertNotNull(issuedAt);
    }

    @Test
    void testValidateTokenSuccess() {
        // Arrange
        String token = jwtUtil.generateToken(testUser);

        // Act
        boolean isValid = jwtUtil.validateToken(token, testUser);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void testValidateTokenInvalidUser() {
        // Arrange
        String token = jwtUtil.generateToken(testUser);
        UserDetails differentUser = User.builder()
            .username("different@example.com")
            .password("password")
            .roles("USER")
            .build();

        // Act
        boolean isValid = jwtUtil.validateToken(token, differentUser);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void testValidateTokenFailureWithInvalidToken() {
        // Arrange
        String invalidToken = "invalid.token.here";

        // Act & Assert
        assertThrows(Exception.class, () -> jwtUtil.validateToken(invalidToken, testUser));
    }

    @Test
    void testTokenNotExpiredImmediatelyAfterGeneration() {
        // Arrange
        String token = jwtUtil.generateToken(testUser);

        // Act & Assert
        // Should not throw an exception
        assertDoesNotThrow(() -> jwtUtil.validateToken(token, testUser));
    }
}
