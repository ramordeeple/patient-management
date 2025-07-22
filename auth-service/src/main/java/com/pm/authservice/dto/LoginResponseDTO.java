/**
 * Data Transfer Object representing the login response,
 * containing the JWT token issued upon successful authentication.
 */

package com.pm.authservice.dto;

public class LoginResponseDTO {
    private final String token;

    public LoginResponseDTO(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
