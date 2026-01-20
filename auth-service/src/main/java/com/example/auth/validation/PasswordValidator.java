package com.example.auth.validation;

import org.springframework.stereotype.Component;

/**
 * Validates password strength requirements.
 */
@Component
public class PasswordValidator {

    private static final int MIN_LENGTH = 8;
    private static final String PASSWORD_PATTERN = 
        "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";

    /**
     * Validates password strength.
     * 
     * Requirements:
     * - At least 8 characters long
     * - At least one uppercase letter (A-Z)
     * - At least one lowercase letter (a-z)
     * - At least one digit (0-9)
     * - At least one special character (@#$%^&+=!)
     * - No whitespace
     * 
     * @param password the password to validate
     * @return true if password meets all requirements
     */
    public boolean isValid(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            return false;
        }
        return password.matches(PASSWORD_PATTERN);
    }

    /**
     * Returns a message describing password validation requirements.
     */
    public String getValidationMessage() {
        return "Password must be at least 8 characters long and contain at least one uppercase letter, " +
               "one lowercase letter, one digit, and one special character (@#$%^&+=!)";
    }
}
