package com.weather.controller.temp;

import com.weather.common.Result;
import com.weather.common.ResultUtils;
import com.weather.entity.temp.TempRanking;
import com.weather.entity.temp.TempTrend;
import com.weather.service.temp.TempRankingService;
import com.weather.service.temp.TempTrendService;
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
@RequestMapping("/api/temp")
@RequiredArgsConstructor
public class TempController {

    private final TempRankingService tempRankingService;
    private final TempTrendService tempTrendService;

    @GetMapping("/ranking")
    public Result<List<TempRanking>> ranking(
            @RequestParam(required = false)
            @Min(value = 2000, message = "年份不能早于2000年")
            @Max(value = 2020, message = "年份不能晚于2020年")
            Integer year) {
        return ResultUtils.success(tempRankingService.listByYear(year));
    }

    @GetMapping("/trend")
    public Result<List<TempTrend>> trend(
            @RequestParam @NotBlank(message = "城市不能为空") String city) {
        return ResultUtils.success(tempTrendService.listByCity(city));
    }
}
