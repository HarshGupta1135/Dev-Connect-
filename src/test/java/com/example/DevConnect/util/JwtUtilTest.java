package com.example.DevConnect.util;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure unit test for token creation and rejection - no Spring context required.
 */
public class JwtUtilTest {

    private static final String SECRET = "unit-test-secret-key-that-is-long-enough-1234567890";

    private static UserDetails user(String email) {
        return new User(email, "irrelevant", List.of());
    }

    @Test
    public void generatedTokenRoundTripsTheUsername() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 60_000);
        String token = jwtUtil.generateToken(user("dev@gmail.com"));

        assertEquals("dev@gmail.com", jwtUtil.extractUsername(token));
        assertTrue(jwtUtil.isTokenValid(token, user("dev@gmail.com")));
    }

    @Test
    public void tokenOfAnotherUserIsNotValid() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 60_000);
        String token = jwtUtil.generateToken(user("dev@gmail.com"));

        assertFalse(jwtUtil.isTokenValid(token, user("someone.else@gmail.com")));
    }

    @Test
    public void expiredTokenIsRejected() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, -1_000); // already expired on creation
        String token = jwtUtil.generateToken(user("dev@gmail.com"));

        assertThrows(ExpiredJwtException.class, () -> jwtUtil.extractUsername(token));
    }

    @Test
    public void tokenSignedWithAnotherSecretIsRejected() {
        String token = new JwtUtil(SECRET, 60_000).generateToken(user("dev@gmail.com"));
        JwtUtil otherInstance = new JwtUtil("a-completely-different-secret-key-0987654321-xyz", 60_000);

        assertThrows(JwtException.class, () -> otherInstance.extractUsername(token));
    }

    @Test
    public void tooShortSecretIsRejectedAtStartup() {
        assertThrows(IllegalStateException.class, () -> new JwtUtil("too-short", 60_000));
    }
}
