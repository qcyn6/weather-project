package com.weather.controller.sun;

import com.weather.common.Result;
import com.weather.common.ResultUtils;
import com.weather.entity.sun.SunRanking;
import com.weather.entity.sun.SunTrend;
import com.weather.service.sun.SunRankingService;
import com.weather.service.sun.SunTrendService;
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
@RequestMapping("/api/sun")
@RequiredArgsConstructor
public class SunController {

    private final SunRankingService sunRankingService;
    private final SunTrendService sunTrendService;

    @GetMapping("/ranking")
    public Result<List<SunRanking>> ranking(
            @RequestParam(required = false)
            @Min(value = 2000, message = "年份不能早于2000年")
            @Max(value = 2020, message = "年份不能晚于2020年")
            Integer year) {
        return ResultUtils.success(sunRankingService.listByYear(year));
    }

    @GetMapping("/trend")
    public Result<List<SunTrend>> trend(
            @RequestParam @NotBlank(message = "城市不能为空") String city) {
        return ResultUtils.success(sunTrendService.listByCity(city));
    }
}
