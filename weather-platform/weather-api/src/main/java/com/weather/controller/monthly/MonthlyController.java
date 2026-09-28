package com.weather.controller.monthly;

import com.weather.common.BusinessException;
import com.weather.common.Result;
import com.weather.common.ResultCode;
import com.weather.common.ResultUtils;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 月度分布分析：单城市 21 年 × 12 月的月度序列（dws 月度层）。
 */
@Validated
@RestController
@RequestMapping("/api/monthly")
@RequiredArgsConstructor
public class MonthlyController {

    /** factor -> (月度表, 指标列...) 白名单 */
    private static final Map<String, String[]> FACTORS = Map.of(
            "rain", new String[]{"dws_rain_monthly", "total_rain", "rainy_days"},
            "temp", new String[]{"dws_temp_monthly", "avg_temp", "max_temp", "min_temp"},
            "sun", new String[]{"dws_sun_monthly", "total_sun", "avg_sun"},
            "hum", new String[]{"dws_hum_monthly", "avg_humidity", "max_humidity", "min_humidity"},
            "wind", new String[]{"dws_wind_monthly", "avg_wind", "max_wind"});

    private final JdbcTemplate jdbcTemplate;

    @GetMapping
    public Result<List<Map<String, Object>>> monthly(
            @RequestParam @NotBlank String factor,
            @RequestParam @NotBlank String city) {
        String[] f = FACTORS.get(factor);
        if (f == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "不支持的气象要素");
        }
        String cols = "`year_month`, " + String.join(", ", f[1]);
        String sql = "SELECT " + cols + " FROM " + f[0] + " WHERE city = ? ORDER BY `year_month`";
        return ResultUtils.success(jdbcTemplate.queryForList(sql, city));
    }
}
