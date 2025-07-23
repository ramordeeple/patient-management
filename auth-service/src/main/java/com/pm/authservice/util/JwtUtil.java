/**
 Utility class for working with JWT tokens.
 Handles token creation and validation using a secret key.
 */

package com.pm.authservice.util;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtUtil {
    private final Key secretKey;

    /**
     Initializes the secret key used to sign and validate JWTs.
     The key is expected to be Base64-encoded and injected via application properties.
     */
    public JwtUtil(@Value("${jwt.secret}")  String secret) {
        /// Decode the Base64-encoded secret key from application config
        byte[] keyBytes = Base64.getDecoder().decode(secret.getBytes(StandardCharsets.UTF_8));
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     Generates a JWT token for a given email and role.
     The token is signed with the secret key and expires in 10 hours.
     */
    public String generateToken(String email, String role) {
        return Jwts.builder()
                .subject(email) /// sets subject (usually user identifier)
                .claim("role", role) /// adds custom claim: user role
                .issuedAt(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)) ///sets issue time (10 hours ahead)
                .signWith(secretKey) /// signs the token with our secret key
                .compact(); /// builds the final JWT string

    }

    /**
     Validates a JWT token.
     If the signature is invalid or the token is malformed, throws a JwtException.
     */
    public void validateToken(String token) {
        try {
            /// Parse and verify the token using the secret key
            Jwts.parser()
                    .verifyWith((SecretKey) secretKey) /// verifies signature with the secret key
                    .build() /// builds the parser configuration (ready to parse token)
                    .parseClaimsJws(token); /// parses and validate the JWT

        } catch(SignatureException e) {
            /// Signature does not match
            throw new JwtException("Invalid JWT signature");
        } catch(JwtException e) {
            /// Other issues with the token (e.g., malformed or expired)
            throw new JwtException("Invalid JWT");
        }
    }
}
