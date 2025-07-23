/**
 Service for authenticating users and validating JWT tokens.

 Handles user credential verification by checking email and password,
 issues JWT tokens upon successful login,
 and validates tokens for subsequent requests.
 */

package com.pm.authservice.service;

import com.pm.authservice.dto.LoginRequestDTO;
import com.pm.authservice.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     Constructor initializing service dependencies:
     UserService for user management,
     PasswordEncoder for password verification,
     JwtUtil for JWT token operations.
     */
    public AuthService(UserService userService, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     Authenticates a user:
     1. Accepts a DTO with login credentials,
     2. tries to find the user by email,
     3. verifies the hashed password,
     4. if valid — generates a JWT token containing email and role,
     5. returns an Optional with the token or empty if authentication fails.
     */
    public Optional<String> authenticate(LoginRequestDTO loginRequestDTO) {
        Optional<String> token = userService
                .findByEmail(loginRequestDTO.getEmail()) /** find user by email */
                .filter(u -> passwordEncoder.matches(loginRequestDTO.getPassword(),
                        u.getPassword())) /** verify password */
                .map(u -> jwtUtil.generateToken(u.getEmail(), u.getRole())); /** For generating jwt token with email and role */
        return token;
    }
    /**
     Validates a JWT token:
     1. Calls JwtUtil to verify the token's integrity and expiration,
     2. returns true if token is valid, or false if the token is invalid or expired.
     */
    public boolean validateToken(String token) {
        try {
            jwtUtil.validateToken(token);
            return  true;
        } catch (JwtException e) {
            return false;
        }
    }
}
