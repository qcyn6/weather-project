package com.weather.controller.health;

import com.weather.common.Result;
import com.weather.common.ResultUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 服务自检：探活 + 数据库连通性，供侧边栏状态灯轮询（无需登录）。
 */
@RestController
@RequiredArgsConstructor
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/api/health")
    public Result<Map<String, String>> health() {
        String db = "up";
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        } catch (Exception e) {
            db = "down";
        }
        return ResultUtils.success(Map.of("status", "up", "db", db));
    }
}
