package com.weather.controller.hum;

import com.weather.common.Result;
import com.weather.common.ResultUtils;
import com.weather.entity.hum.HumRanking;
import com.weather.entity.hum.HumTrend;
import com.weather.service.hum.HumRankingService;
import com.weather.service.hum.HumTrendService;
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
@RequestMapping("/api/hum")
@RequiredArgsConstructor
public class HumController {

    private final HumRankingService humRankingService;
    private final HumTrendService humTrendService;

    @GetMapping("/ranking")
    public Result<List<HumRanking>> ranking(
            @RequestParam(required = false)
            @Min(value = 2000, message = "年份不能早于2000年")
            @Max(value = 2020, message = "年份不能晚于2020年")
            Integer year) {
        return ResultUtils.success(humRankingService.listByYear(year));
    }

    @GetMapping("/trend")
    public Result<List<HumTrend>> trend(
            @RequestParam @NotBlank(message = "城市不能为空") String city) {
        return ResultUtils.success(humTrendService.listByCity(city));
    }
}
