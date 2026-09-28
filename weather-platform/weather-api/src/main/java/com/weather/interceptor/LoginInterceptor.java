package com.weather.interceptor;

import cn.hutool.core.convert.Convert;
import cn.hutool.jwt.JWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weather.common.BusinessException;
import com.weather.common.JwtUtil;
import com.weather.common.Result;
import com.weather.common.ResultCode;
import com.weather.common.ResultUtils;
import com.weather.common.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class LoginInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // CORS 预检请求不带 token，直接放行
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            writeUnauthorized(response, ResultCode.UNAUTHORIZED.getMsg());
            return false;
        }
        try {
            JWT jwt = jwtUtil.verifyToken(auth.substring(7));
            UserContext.set(new UserContext.Principal(
                    Convert.toLong(jwt.getPayload("userId")),
                    Convert.toStr(jwt.getPayload("username")),
                    Convert.toStr(jwt.getPayload("role"))));
            return true;
        } catch (BusinessException e) {
            writeUnauthorized(response, e.getMessage());
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private void writeUnauthorized(HttpServletResponse response, String msg) throws Exception {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Result<Void> result = ResultUtils.fail(ResultCode.UNAUTHORIZED.getCode(), msg);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
