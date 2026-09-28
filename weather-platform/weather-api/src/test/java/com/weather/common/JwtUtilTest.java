package com.weather.common;

import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.signers.JWTSignerUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtUtilTest {

    private static final String SECRET = "test-secret-key-at-least-32-bytes-long!!";
    private static final byte[] KEY = SECRET.getBytes(StandardCharsets.UTF_8);

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expireHours", 24L);
    }

    @Test
    void createThenVerifyShouldReturnPayload() {
        String token = jwtUtil.createToken(1L, "admin", "admin");
        JWT jwt = jwtUtil.verifyToken(token);

        assertEquals(1L, Long.valueOf(String.valueOf(jwt.getPayload("userId"))));
        assertEquals("admin", String.valueOf(jwt.getPayload("username")));
        assertEquals("admin", String.valueOf(jwt.getPayload("role")));
    }

    @Test
    void blankTokenShouldBeRejected() {
        assertThrows(BusinessException.class, () -> jwtUtil.verifyToken(null));
        assertThrows(BusinessException.class, () -> jwtUtil.verifyToken("  "));
    }

    @Test
    void forgedTokenShouldBeRejected() {
        String forged = JWTUtil.createToken(
                java.util.Map.of("userId", 1L, "username", "admin"),
                "another-secret-key-at-least-32-bytes-long!!".getBytes(StandardCharsets.UTF_8));
        BusinessException ex = assertThrows(BusinessException.class, () -> jwtUtil.verifyToken(forged));
        assertEquals(ResultCode.UNAUTHORIZED.getCode(), ex.getCode());
    }

    @Test
    void expiredTokenShouldBeRejected() {
        String expired = JWT.create()
                .setPayload("userId", 1L)
                .setPayload("username", "admin")
                .setPayload("exp", System.currentTimeMillis() / 1000 - 3600)
                .sign(JWTSignerUtil.hs256(KEY));
        BusinessException ex = assertThrows(BusinessException.class, () -> jwtUtil.verifyToken(expired));
        assertEquals(ResultCode.UNAUTHORIZED.getCode(), ex.getCode());
    }
}
