package com.weather.service;

import com.weather.dto.LoginResponse;

public interface SysUserService {

    Long register(String username, String password, String nickname);

    LoginResponse login(String username, String password);

    LoginResponse getCurrentUser();
}
