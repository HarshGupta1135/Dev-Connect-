package com.example.DevConnect.util;

import com.example.DevConnect.service.UserPrincipal;
import com.example.DevConnect.support.TestData;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    private static final String SECRET = TestData.JWT_SECRET;

    private JwtUtil jwtUtil;
    private UserPrincipal harsh;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secretKey", SECRET);
        harsh = new UserPrincipal(TestData.user(1L, "harsh@gmail.com", "DEVELOPER"));
    }

    @Test
    void tokenSubjectIsTheEmail() {
        String token = jwtUtil.generateToken(harsh);

        assertEquals("harsh@gmail.com", jwtUtil.extractUsername(token));
    }

    @Test
    void tokenExpiresInTenHours() {
        String token = jwtUtil.generateToken(harsh);

        long hours = (jwtUtil.extractExpiration(token).getTime() - System.currentTimeMillis()) / 3_600_000;
        assertTrue(hours >= 9 && hours <= 10, "expected ~10 hours, got " + hours);
    }

    @Test
    void tokenIsValidForItsOwnUser() {
        assertTrue(jwtUtil.isTokenValid(jwtUtil.generateToken(harsh), harsh));
    }

    @Test
    void tokenIsNotValidForAnotherUser() {
        UserPrincipal other = new UserPrincipal(TestData.user(2L, "other@gmail.com", "DEVELOPER"));

        assertFalse(jwtUtil.isTokenValid(jwtUtil.generateToken(harsh), other));
    }

    @Test
    void expiredTokenIsRejected() {
        String expired = Jwts.builder()
                .setSubject("harsh@gmail.com")
                .setIssuedAt(new Date(System.currentTimeMillis() - 20_000))
                .setExpiration(new Date(System.currentTimeMillis() - 10_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes()), SignatureAlgorithm.HS256)
                .compact();

        assertThrows(ExpiredJwtException.class, () -> jwtUtil.extractUsername(expired));
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        JwtUtil otherServer = new JwtUtil();
        ReflectionTestUtils.setField(otherServer, "secretKey", "a-completely-different-secret-of-32-chars!!");
        String forged = otherServer.generateToken(harsh);

        assertThrows(SignatureException.class, () -> jwtUtil.extractUsername(forged));
    }

    @Test
    void tamperedPayloadIsRejected() {
        String[] parts = jwtUtil.generateToken(harsh).split("\\.");
        String otherPayload = jwtUtil.generateToken(new UserPrincipal(TestData.user(2L, "admin@gmail.com", "ADMIN")))
                .split("\\.")[1];
        String tampered = parts[0] + "." + otherPayload + "." + parts[2];

        assertThrows(SignatureException.class, () -> jwtUtil.extractUsername(tampered));
    }
}
