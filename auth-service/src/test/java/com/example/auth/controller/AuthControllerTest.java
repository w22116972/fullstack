package com.example.auth.controller;

import com.example.auth.dto.AuthResponse;
import com.example.auth.dto.LoginRequest;
import com.example.auth.dto.RegisterRequest;
import com.example.auth.dto.TokenValidationRequest;
import com.example.auth.exception.AuthException;
import com.example.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;
    private TokenValidationRequest tokenValidationRequest;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("TestPassword123!");

        registerRequest = new RegisterRequest();
        registerRequest.setEmail("newuser@example.com");
        registerRequest.setPassword("TestPassword123!");

        tokenValidationRequest = new TokenValidationRequest();
        tokenValidationRequest.setToken("jwt-token-123");
    }

    @Test
    void testLoginSuccess() throws Exception {
        // Arrange
        AuthResponse response = new AuthResponse(
            "jwt-token-123",
            "test@example.com",
            "test@example.com",
            "USER"
        );
        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token", equalTo("jwt-token-123")))
            .andExpect(jsonPath("$.email", equalTo("test@example.com")))
            .andExpect(jsonPath("$.role", equalTo("USER")));

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    void testLoginFailureInvalidCredentials() throws Exception {
        // Arrange
        when(authService.login(any(LoginRequest.class)))
            .thenThrow(new AuthException("Invalid email or password"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error", containsString("Invalid")));

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    void testRegisterSuccess() throws Exception {
        // Arrange
        AuthResponse response = new AuthResponse(
            "jwt-token-456",
            "newuser@example.com",
            "newuser@example.com",
            "USER"
        );
        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token", equalTo("jwt-token-456")))
            .andExpect(jsonPath("$.email", equalTo("newuser@example.com")))
            .andExpect(jsonPath("$.role", equalTo("USER")));

        verify(authService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    void testRegisterFailureWeakPassword() throws Exception {
        // Arrange
        when(authService.register(any(RegisterRequest.class)))
            .thenThrow(new AuthException("Password must be at least 8 characters"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error", containsString("Password")));

        verify(authService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    void testRegisterFailureEmailAlreadyExists() throws Exception {
        // Arrange
        when(authService.register(any(RegisterRequest.class)))
            .thenThrow(new AuthException("Email already registered"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error", containsString("Email")));

        verify(authService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    void testValidateTokenSuccess() throws Exception {
        // Arrange
        when(authService.validateToken("jwt-token-123")).thenReturn(true);
        when(authService.extractUsername("jwt-token-123")).thenReturn("test@example.com");

        // Act & Assert
        mockMvc.perform(post("/api/auth/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tokenValidationRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid", equalTo(true)))
            .andExpect(jsonPath("$.username", equalTo("test@example.com")));

        verify(authService, times(1)).validateToken("jwt-token-123");
    }

    @Test
    void testValidateTokenFailure() throws Exception {
        // Arrange
        when(authService.validateToken("invalid-token")).thenReturn(false);
        tokenValidationRequest.setToken("invalid-token");

        // Act & Assert
        mockMvc.perform(post("/api/auth/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tokenValidationRequest)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.valid", equalTo(false)));

        verify(authService, times(1)).validateToken("invalid-token");
    }

    @Test
    void testLoginMissingEmail() throws Exception {
        // Arrange
        loginRequest.setEmail(null);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testRegisterMissingPassword() throws Exception {
        // Arrange
        registerRequest.setPassword(null);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isBadRequest());
    }
}
