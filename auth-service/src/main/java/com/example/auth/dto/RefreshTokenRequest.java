package com.example.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for token refresh operations.
 */
public class RefreshTokenRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;

    public RefreshTokenRequest() {
    }

    public RefreshTokenRequest(String username, String refreshToken) {
        this.username = username;
        this.refreshToken = refreshToken;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
