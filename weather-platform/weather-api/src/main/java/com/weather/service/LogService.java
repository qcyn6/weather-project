package com.weather.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 操作日志：写 sys_operation_log 表。日志失败不影响主流程。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogService {

    private final JdbcTemplate jdbcTemplate;

    public void log(String username, String action, String detail) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO sys_operation_log (username, action, detail) VALUES (?, ?, ?)",
                    username, action, detail);
        } catch (Exception e) {
            log.warn("写入操作日志失败: {} {} {}", username, action, e.getMessage());
        }
    }
}
