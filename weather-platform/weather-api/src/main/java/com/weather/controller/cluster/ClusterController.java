package com.weather.controller.cluster;

import com.weather.common.Result;
import com.weather.common.ResultUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 城市气候聚类（Spark KMeans）结果查询。
 */
@RestController
@RequestMapping("/api/cluster")
@RequiredArgsConstructor
public class ClusterController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/result")
    public Result<Map<String, Object>> result() {
        return ResultUtils.success(Map.of(
                "clusters", jdbcTemplate.queryForList(
                        "SELECT cluster_id, cluster_name, city_count, avg_rain, avg_temp, avg_sun, avg_hum, avg_wind "
                                + "FROM ads_cluster_profile ORDER BY cluster_id"),
                "cities", jdbcTemplate.queryForList(
                        "SELECT c.city, c.province, c.cluster_id, c.cluster_name, g.lat, g.lon "
                                + "FROM ads_city_cluster c LEFT JOIN dim_city_geo g ON g.city = c.city "
                                + "ORDER BY c.cluster_id, c.city")));
    }
}
