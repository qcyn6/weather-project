package com.weather.controller.export;

import com.alibaba.excel.EasyExcel;
import com.weather.common.BusinessException;
import com.weather.common.ResultCode;
import com.weather.common.UserContext;
import com.weather.service.LogService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Excel 报表导出：排名榜 / 城市趋势，EasyExcel 动态表头。
 */
@Validated
@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class ExportController {

    private final JdbcTemplate jdbcTemplate;
    private final LogService logService;

    /** factor -> [排名表, trend表, 指标列..., 表头...]（白名单） */
    private static final Map<String, String[][]> FACTORS = Map.of(
            "rain", new String[][]{{"ads_rain_ranking", "ads_rain_trend"},
                    {"total_rain", "rainy_days"}, {"降水总量(mm)", "降雨天数(天)"}, {"降水"}},
            "temp", new String[][]{{"ads_temp_ranking", "ads_temp_trend"},
                    {"avg_temp", "max_temp", "min_temp"}, {"平均气温(°C)", "最高气温(°C)", "最低气温(°C)"}, {"气温"}},
            "sun", new String[][]{{"ads_sun_ranking", "ads_sun_trend"},
                    {"total_sun", "avg_sun", "sunny_days"}, {"日照总时数(h)", "日均日照(h)", "晴天数(天)"}, {"日照"}},
            "hum", new String[][]{{"ads_hum_ranking", "ads_hum_trend"},
                    {"avg_humidity", "max_humidity", "min_humidity"}, {"平均湿度(%)", "最大湿度(%)", "最小湿度(%)"}, {"湿度"}},
            "wind", new String[][]{{"ads_wind_ranking", "ads_wind_trend"},
                    {"avg_wind", "max_wind", "gale_days"}, {"平均风速(m/s)", "最大风速(m/s)", "大风天数(天)"}, {"风速"}});

    @GetMapping("/ranking")
    public ResponseEntity<byte[]> exportRanking(
            @RequestParam @NotBlank String factor,
            @RequestParam @Min(2000) @Max(2020) Integer year) {
        String[][] f = factor(factor);
        String sql = "SELECT rank_num, city, province, " + String.join(", ", f[1])
                + " FROM " + f[0][0] + " WHERE year = ? ORDER BY rank_num";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, year);

        List<List<String>> head = new ArrayList<>();
        head.add(List.of("排名"));
        head.add(List.of("城市"));
        head.add(List.of("省份"));
        for (String h : f[2]) {
            head.add(List.of(h));
        }
        List<List<Object>> data = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            List<Object> line = new ArrayList<>();
            line.add(row.get("rank_num"));
            line.add(row.get("city"));
            line.add(row.get("province"));
            for (String col : f[1]) {
                line.add(row.get(col));
            }
            data.add(line);
        }
        String fileName = f[3][0] + "城市排名_" + year + ".xlsx";
        logService.log(currentUser(), "EXPORT", "导出" + f[3][0] + "排名 " + year + " 年，" + rows.size() + " 行");
        return xlsx(fileName, head, data);
    }

    @GetMapping("/trend")
    public ResponseEntity<byte[]> exportTrend(
            @RequestParam @NotBlank String factor,
            @RequestParam @NotBlank String city) {
        String[][] f = factor(factor);
        String sql = "SELECT year, city, trend_label, " + String.join(", ", f[1])
                + " FROM " + f[0][1] + " WHERE city = ? ORDER BY year";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, city);

        List<List<String>> head = new ArrayList<>();
        head.add(List.of("年份"));
        head.add(List.of("城市"));
        head.add(List.of("同比趋势"));
        for (String h : f[2]) {
            head.add(List.of(h));
        }
        List<List<Object>> data = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            List<Object> line = new ArrayList<>();
            line.add(row.get("year"));
            line.add(row.get("city"));
            line.add(row.get("trend_label"));
            for (String col : f[1]) {
                line.add(row.get(col));
            }
            data.add(line);
        }
        String fileName = city + f[3][0] + "趋势_2000-2020.xlsx";
        logService.log(currentUser(), "EXPORT", "导出" + city + f[3][0] + "趋势，" + rows.size() + " 行");
        return xlsx(fileName, head, data);
    }

    private String[][] factor(String factor) {
        String[][] f = FACTORS.get(factor);
        if (f == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "不支持的气象要素");
        }
        return f;
    }

    private String currentUser() {
        UserContext.Principal p = UserContext.get();
        return p == null ? "unknown" : p.username();
    }

    private ResponseEntity<byte[]> xlsx(String fileName, List<List<String>> head, List<List<Object>> data) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        EasyExcel.write(bos).head(head).sheet("数据").doWrite(data);
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename*=UTF-8''" + encoded)
                .contentType(org.springframework.http.MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bos.toByteArray());
    }
}
