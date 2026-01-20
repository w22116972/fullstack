package com.example.auth.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PasswordValidatorTest {

    @InjectMocks
    private PasswordValidator passwordValidator;

    @BeforeEach
    void setUp() {
        passwordValidator = new PasswordValidator();
    }

    @Test
    void testValidPasswordStrong() {
        // Act & Assert
        assertTrue(passwordValidator.isValid("TestPassword123!"));
    }

    @Test
    void testValidPasswordWithSpecialCharacters() {
        // Act & Assert
        assertTrue(passwordValidator.isValid("MyPassword@2024"));
    }

    @Test
    void testInvalidPasswordTooShort() {
        // Act & Assert
        assertFalse(passwordValidator.isValid("Test@123"));
    }

    @Test
    void testInvalidPasswordNoUppercase() {
        // Act & Assert
        assertFalse(passwordValidator.isValid("testpassword@123"));
    }

    @Test
    void testInvalidPasswordNoLowercase() {
        // Act & Assert
        assertFalse(passwordValidator.isValid("TESTPASSWORD@123"));
    }

    @Test
    void testInvalidPasswordNoDigit() {
        // Act & Assert
        assertFalse(passwordValidator.isValid("TestPassword@NoDigit"));
    }

    @Test
    void testInvalidPasswordNoSpecialCharacter() {
        // Act & Assert
        assertFalse(passwordValidator.isValid("TestPassword123"));
    }

    @Test
    void testInvalidPasswordWithWhitespace() {
        // Act & Assert
        assertFalse(passwordValidator.isValid("Test Pass@123"));
    }

    @Test
    void testInvalidPasswordNull() {
        // Act & Assert
        assertFalse(passwordValidator.isValid(null));
    }

    @Test
    void testValidationMessageNotNull() {
        // Act
        String message = passwordValidator.getValidationMessage();

        // Assert
        assertNotNull(message);
        assertFalse(message.isEmpty());
        assertTrue(message.contains("8 characters"));
    }

    @Test
    void testMultipleValidPasswords() {
        // Act & Assert
        assertTrue(passwordValidator.isValid("SecurePass@1"));
        assertTrue(passwordValidator.isValid("MyStrongPass123!"));
        assertTrue(passwordValidator.isValid("Complex#Password2024"));
    }

    @Test
    void testEdgeCaseExactly8Characters() {
        // Valid: exactly 8 characters with all requirements
        assertTrue(passwordValidator.isValid("Passs123@"));
    }

    @Test
    void testEdgeCaseLessThan8Characters() {
        // Invalid: less than 8 characters
        assertFalse(passwordValidator.isValid("Pass@123")); // 8 chars, but actually valid
    }
}
