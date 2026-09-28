package com.weather.controller.dashboard;

import com.weather.common.BusinessException;
import com.weather.common.Result;
import com.weather.common.ResultCode;
import com.weather.common.ResultUtils;
import com.weather.dto.dashboard.DashboardSummaryVO;
import com.weather.service.dashboard.DashboardService;
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

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Validated
public class DashboardController {

    private final DashboardService dashboardService;
    private final JdbcTemplate jdbcTemplate;

    /** factor -> (trend表, 指标列)，白名单防注入 */
    private static final Map<String, String[]> TREND_FACTORS = Map.of(
            "rain", new String[]{"ads_rain_trend", "total_rain"},
            "temp", new String[]{"ads_temp_trend", "avg_temp"},
            "sun", new String[]{"ads_sun_trend", "total_sun"},
            "hum", new String[]{"ads_hum_trend", "avg_humidity"},
            "wind", new String[]{"ads_wind_trend", "avg_wind"});

    @GetMapping("/summary")
    public Result<DashboardSummaryVO> summary() {
        return ResultUtils.success(dashboardService.getSummary());
    }

    /** 全国均值逐年演变（大屏底部图），21 行聚合结果 */
    @GetMapping("/national-trend")
    public Result<List<Map<String, Object>>> nationalTrend(@RequestParam @NotBlank String factor) {
        String[] f = TREND_FACTORS.get(factor);
        if (f == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "不支持的气象要素");
        }
        String sql = "SELECT year, ROUND(AVG(" + f[1] + "), 2) AS value FROM " + f[0]
                + " GROUP BY year ORDER BY year";
        return ResultUtils.success(jdbcTemplate.queryForList(sql));
    }
}
