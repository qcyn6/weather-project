package com.weather.controller.rain;

import com.weather.common.Result;
import com.weather.common.ResultUtils;
import com.weather.entity.rain.RainRanking;
import com.weather.entity.rain.RainTrend;
import com.weather.service.rain.RainRankingService;
import com.weather.service.rain.RainTrendService;
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
@RequestMapping("/api/rain")
@RequiredArgsConstructor
public class RainController {

    private final RainRankingService rainRankingService;
    private final RainTrendService rainTrendService;

    @GetMapping("/ranking")
    public Result<List<RainRanking>> ranking(
            @RequestParam(required = false)
            @Min(value = 2000, message = "年份不能早于2000年")
            @Max(value = 2020, message = "年份不能晚于2020年")
            Integer year) {
        return ResultUtils.success(rainRankingService.listByYear(year));
    }

    @GetMapping("/trend")
    public Result<List<RainTrend>> trend(
            @RequestParam @NotBlank(message = "城市不能为空") String city) {
        return ResultUtils.success(rainTrendService.listByCity(city));
    }
}
