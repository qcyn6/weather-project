package com.weather.common;

import cn.hutool.core.convert.Convert;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expire-hours}")
    private long expireHours;

    public String createToken(Long userId, String username, String role) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("username", username);
        payload.put("role", role);
        payload.put("exp", System.currentTimeMillis() / 1000 + expireHours * 3600);
        return JWTUtil.createToken(payload, secret.getBytes(StandardCharsets.UTF_8));
    }

    public JWT verifyToken(String token) {
        if (token == null || token.isBlank()) {
            throw unauthorized();
        }
        boolean valid;
        try {
            valid = JWTUtil.verify(token, secret.getBytes(StandardCharsets.UTF_8));
        } catch (RuntimeException e) {
            // 格式非法的 token 在解析阶段会直接抛异常
            throw unauthorized();
        }
        if (!valid) {
            throw unauthorized();
        }
        JWT jwt = JWTUtil.parseToken(token);
        Long exp = Convert.toLong(jwt.getPayload("exp"));
        if (exp == null || System.currentTimeMillis() / 1000 > exp) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "登录已过期");
        }
        return jwt;
    }

    private BusinessException unauthorized() {
        return new BusinessException(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMsg());
    }
}
