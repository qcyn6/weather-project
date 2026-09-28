package com.weather.controller.geo;

import com.weather.common.BusinessException;
import com.weather.common.Result;
import com.weather.common.ResultCode;
import com.weather.common.ResultUtils;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
 * 气候地图：城市经纬度维表 JOIN 要素排名表，返回地图打点数据。
 * 表名/列名来自白名单，不拼接用户输入，无注入风险。
 */
@Validated
@RestController
@RequestMapping("/api/geo")
@RequiredArgsConstructor
public class GeoController {

    /** factor -> (排名表, 主指标列) */
    private static final Map<String, String[]> FACTORS = Map.of(
            "rain", new String[]{"ads_rain_ranking", "total_rain"},
            "temp", new String[]{"ads_temp_ranking", "avg_temp"},
            "sun", new String[]{"ads_sun_ranking", "total_sun"},
            "hum", new String[]{"ads_hum_ranking", "avg_humidity"},
            "wind", new String[]{"ads_wind_ranking", "avg_wind"});

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/values")
    public Result<List<Map<String, Object>>> values(
            @RequestParam @NotBlank String factor,
            @RequestParam @Min(2000) @Max(2020) Integer year) {
        String[] f = FACTORS.get(factor);
        if (f == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "不支持的气象要素");
        }
        String sql = "SELECT g.city, g.province, g.lat, g.lon, r.rank_num, r." + f[1]
                + " AS value FROM dim_city_geo g JOIN " + f[0]
                + " r ON r.city = g.city WHERE r.year = ? ORDER BY r." + f[1] + " DESC";
        return ResultUtils.success(jdbcTemplate.queryForList(sql, year));
    }

    /** 省级聚合：按省取城市均值，供地图整省着色 */
    @GetMapping("/province-values")
    public Result<List<Map<String, Object>>> provinceValues(
            @RequestParam @NotBlank String factor,
            @RequestParam @Min(2000) @Max(2020) Integer year) {
        String[] f = FACTORS.get(factor);
        if (f == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "不支持的气象要素");
        }
        String sql = "SELECT province, ROUND(AVG(" + f[1] + "), 2) AS value, COUNT(*) AS cities FROM "
                + f[0] + " WHERE year = ? GROUP BY province ORDER BY value DESC";
        return ResultUtils.success(jdbcTemplate.queryForList(sql, year));
    }
}
