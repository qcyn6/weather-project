package com.weather.controller.manage;

import com.weather.common.BusinessException;
import com.weather.common.Result;
import com.weather.common.ResultCode;
import com.weather.common.ResultUtils;
import com.weather.common.UserContext;
import com.weather.service.LogService;
import com.weather.service.ReclusterService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据管理（仅 admin）：ADS 表行数统计、操作日志、SSH 触发聚类重跑。
 */
@RestController
@RequestMapping("/api/manage")
@RequiredArgsConstructor
public class ManageController {

    private static final List<String> TABLES = List.of(
            "ads_rain_ranking", "ads_rain_trend",
            "ads_temp_ranking", "ads_temp_trend",
            "ads_sun_ranking", "ads_sun_trend",
            "ads_hum_ranking", "ads_hum_trend",
            "ads_wind_ranking", "ads_wind_trend",
            "dim_city_geo", "ads_city_cluster", "ads_cluster_profile",
            "sys_user", "sys_operation_log");

    private final JdbcTemplate jdbcTemplate;
    private final ReclusterService reclusterService;
    private final LogService logService;

    @GetMapping("/stats")
    public Result<List<Map<String, Object>>> stats() {
        requireAdmin();
        List<Map<String, Object>> result = new ArrayList<>();
        for (String table : TABLES) {
            Long rows = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("table", table);
            item.put("rows", rows);
            result.add(item);
        }
        return ResultUtils.success(result);
    }

    @GetMapping("/logs")
    public Result<List<Map<String, Object>>> logs() {
        requireAdmin();
        return ResultUtils.success(jdbcTemplate.queryForList(
                "SELECT id, username, action, detail, create_time FROM sys_operation_log ORDER BY id DESC LIMIT 50"));
    }

    @PostMapping("/recluster")
    public Result<String> recluster(@RequestParam @NotBlank String token) {
        requireAdmin();
        // token 参数无实际含义，仅防 CSRF 式误触发；真正校验是管理员身份
        String output = reclusterService.recluster();
        UserContext.Principal p = UserContext.get();
        logService.log(p == null ? "unknown" : p.username(), "RECLUSTER", output.substring(0, Math.min(200, output.length())));
        return ResultUtils.success(output);
    }

    private void requireAdmin() {
        UserContext.Principal p = UserContext.get();
        if (p == null || !"admin".equals(p.role())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "仅管理员可访问数据管理功能");
        }
    }
}
