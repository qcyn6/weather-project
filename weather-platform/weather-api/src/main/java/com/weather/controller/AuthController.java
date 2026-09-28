package com.weather.controller;

import com.weather.common.Result;
import com.weather.common.ResultUtils;
import com.weather.dto.LoginRequest;
import com.weather.dto.LoginResponse;
import com.weather.dto.RegisterRequest;
import com.weather.service.SysUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserService sysUserService;

    @PostMapping("/register")
    public Result<Long> register(@Valid @RequestBody RegisterRequest req) {
        Long userId = sysUserService.register(req.getUsername(), req.getPassword(), req.getNickname());
        return ResultUtils.success(userId);
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResultUtils.success(sysUserService.login(req.getUsername(), req.getPassword()));
    }

    @GetMapping("/me")
    public Result<LoginResponse> me() {
        return ResultUtils.success(sysUserService.getCurrentUser());
    }
}
