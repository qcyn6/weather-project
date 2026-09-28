package com.weather.controller.wind;

import com.weather.common.Result;
import com.weather.common.ResultUtils;
import com.weather.entity.wind.WindRanking;
import com.weather.entity.wind.WindTrend;
import com.weather.service.wind.WindRankingService;
import com.weather.service.wind.WindTrendService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/wind")
@RequiredArgsConstructor
public class WindController {

    private final WindRankingService windRankingService;
    private final WindTrendService windTrendService;

    @GetMapping("/ranking")
    public Result<List<WindRanking>> ranking(
            @RequestParam(required = false)
            @Min(value = 2000, message = "年份不能早于2000年")
            @Max(value = 2020, message = "年份不能晚于2020年")
            Integer year) {
        return ResultUtils.success(windRankingService.listByYear(year));
    }

    @GetMapping("/trend")
    public Result<List<WindTrend>> trend(
            @RequestParam @NotBlank(message = "城市不能为空") String city) {
        return ResultUtils.success(windTrendService.listByCity(city));
    }
}
