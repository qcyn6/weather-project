package com.weather.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.weather.common.BusinessException;
import com.weather.common.JwtUtil;
import com.weather.common.ResultCode;
import com.weather.common.UserContext;
import com.weather.dto.LoginResponse;
import com.weather.entity.SysUser;
import com.weather.mapper.SysUserMapper;
import com.weather.service.LogService;
import com.weather.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements SysUserService {

    /** 连续失败锁定：同一用户名 5 次失败锁定 10 分钟（内存态，重启清零） */
    private static final int MAX_FAILS = 5;
    private static final long LOCK_MILLIS = 10 * 60 * 1000L;
    private static final Map<String, FailTrack> LOGIN_FAILS = new ConcurrentHashMap<>();

    private record FailTrack(int count, long firstFailAt) {
    }

    private final SysUserMapper sysUserMapper;
    private final JwtUtil jwtUtil;
    private final LogService logService;

    @Override
    public Long register(String username, String password, String nickname) {
        Long count = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (count > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        user.setNickname(nickname);
        user.setRole("user");
        try {
            sysUserMapper.insert(user);
        } catch (DuplicateKeyException e) {
            // 并发注册同名用户时前置 count 检查拦不住，由唯一索引兜底
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "用户名已存在");
        }
        logService.log(user.getUsername(), "REGISTER", "注册成功");
        return user.getId();
    }

    @Override
    public LoginResponse login(String username, String password) {
        // 锁定检查
        FailTrack track = LOGIN_FAILS.get(username);
        long now = System.currentTimeMillis();
        if (track != null && track.count() >= MAX_FAILS && now - track.firstFailAt() < LOCK_MILLIS) {
            logService.log(username, "LOGIN_LOCKED", "失败次数过多触发锁定");
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "失败次数过多，账号已锁定，请10分钟后再试");
        }

        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null || !checkPassword(password, user.getPassword())) {
            recordFail(username, track, now);
            logService.log(username, "LOGIN_FAIL", "用户名或密码错误");
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "用户名或密码错误");
        }

        LOGIN_FAILS.remove(username);
        String token = jwtUtil.createToken(user.getId(), user.getUsername(), user.getRole());
        logService.log(username, "LOGIN", "登录成功");
        return LoginResponse.builder()
                .token(token)
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .role(user.getRole())
                .build();
    }

    private void recordFail(String username, FailTrack track, long now) {
        int count = (track != null && now - track.firstFailAt() < LOCK_MILLIS) ? track.count() + 1 : 1;
        LOGIN_FAILS.put(username, new FailTrack(count, track == null || count == 1 ? now : track.firstFailAt()));
    }

    private boolean checkPassword(String rawPassword, String hashed) {
        // 兼容占位密文（非BCrypt格式）直接判定失败，避免抛底层异常变成500
        if (hashed == null || !hashed.startsWith("$2")) {
            return false;
        }
        return BCrypt.checkpw(rawPassword, hashed);
    }

    @Override
    public LoginResponse getCurrentUser() {
        UserContext.Principal principal = UserContext.get();
        SysUser user = sysUserMapper.selectById(principal.userId());
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "用户不存在");
        }
        return LoginResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .role(user.getRole())
                .build();
    }
}
