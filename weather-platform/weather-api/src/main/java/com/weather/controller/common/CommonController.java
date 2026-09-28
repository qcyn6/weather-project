package com.weather.controller.common;

import com.weather.common.Result;
import com.weather.common.ResultUtils;
import com.weather.service.rain.RainRankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/common")
@RequiredArgsConstructor
public class CommonController {

    private final RainRankingService rainRankingService;

    @GetMapping("/cities")
    public Result<List<String>> cities() {
        return ResultUtils.success(rainRankingService.listCities());
    }
}
