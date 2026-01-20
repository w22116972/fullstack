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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PasswordValidator passwordValidator;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setPassword("hashedPassword");
        testUser.setRole(Role.USER);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("TestPassword123!");

        registerRequest = new RegisterRequest();
        registerRequest.setEmail("newuser@example.com");
        registerRequest.setPassword("TestPassword123!");
    }

    @Test
    void testLoginSuccess() {
        // Arrange
        String token = "jwt-token-123";
        UsernamePasswordAuthenticationToken authToken = 
            new UsernamePasswordAuthenticationToken("test@example.com", "TestPassword123!", testUser.getAuthorities());
        authToken.setDetails(testUser);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(authToken);
        when(jwtUtil.generateToken(any())).thenReturn(token);

        // Act
        AuthResponse response = authService.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals(token, response.getToken());
        assertEquals("test@example.com", response.getEmail());
        verify(authenticationManager, times(1)).authenticate(any());
        verify(jwtUtil, times(1)).generateToken(any());
    }

    @Test
    void testLoginFailureInvalidCredentials() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenThrow(new BadCredentialsException("Invalid credentials"));

        // Act & Assert
        assertThrows(AuthException.class, () -> authService.login(loginRequest));
        verify(authenticationManager, times(1)).authenticate(any());
    }

    @Test
    void testRegisterSuccess() {
        // Arrange
        String token = "jwt-token-456";
        when(passwordValidator.isValid("TestPassword123!")).thenReturn(true);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode("TestPassword123!")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtil.generateToken(any())).thenReturn(token);

        // Act
        AuthResponse response = authService.register(registerRequest);

        // Assert
        assertNotNull(response);
        assertEquals(token, response.getToken());
        assertEquals("test@example.com", response.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
        verify(jwtUtil, times(1)).generateToken(any());
    }

    @Test
    void testRegisterFailureWeakPassword() {
        // Arrange
        when(passwordValidator.isValid("weak")).thenReturn(false);
        when(passwordValidator.getValidationMessage())
            .thenReturn("Password must be at least 8 characters");

        // Act & Assert
        assertThrows(AuthException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any());
    }

    @Test
    void testRegisterFailureEmailAlreadyExists() {
        // Arrange
        when(passwordValidator.isValid("TestPassword123!")).thenReturn(true);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(true);

        // Act & Assert
        assertThrows(AuthException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any());
    }

    @Test
    void testValidateTokenSuccess() {
        // Arrange
        String token = "valid-token";
        when(jwtUtil.validateToken(token)).thenReturn(true);

        // Act
        boolean result = authService.validateToken(token);

        // Assert
        assertTrue(result);
        verify(jwtUtil, times(1)).validateToken(token);
    }

    @Test
    void testValidateTokenFailure() {
        // Arrange
        String token = "invalid-token";
        when(jwtUtil.validateToken(token)).thenReturn(false);

        // Act
        boolean result = authService.validateToken(token);

        // Assert
        assertFalse(result);
        verify(jwtUtil, times(1)).validateToken(token);
    }

    @Test
    void testExtractUsernameSuccess() {
        // Arrange
        String token = "jwt-token";
        String email = "test@example.com";
        when(jwtUtil.extractUsername(token)).thenReturn(email);

        // Act
        String result = authService.extractUsername(token);

        // Assert
        assertEquals(email, result);
        verify(jwtUtil, times(1)).extractUsername(token);
    }

    @Test
    void testRegisterSetsCorrectUserDefaults() {
        // Arrange
        when(passwordValidator.isValid("TestPassword123!")).thenReturn(true);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode("TestPassword123!")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            assertEquals(Role.USER, savedUser.getRole());
            assertNotNull(savedUser.getCreatedAt());
            assertNotNull(savedUser.getUpdatedAt());
            return savedUser;
        });
        when(jwtUtil.generateToken(any())).thenReturn("token");

        // Act
        authService.register(registerRequest);

        // Assert
        verify(userRepository, times(1)).save(any(User.class));
    }
}
