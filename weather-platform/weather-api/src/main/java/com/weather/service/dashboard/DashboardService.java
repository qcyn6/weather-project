package com.weather.service.dashboard;

import com.weather.dto.dashboard.DashboardSummaryVO;
import com.weather.entity.rain.RainTrend;
import com.weather.entity.temp.TempTrend;
import com.weather.service.hum.HumRankingService;
import com.weather.service.rain.RainRankingService;
import com.weather.service.rain.RainTrendService;
import com.weather.service.sun.SunRankingService;
import com.weather.service.temp.TempRankingService;
import com.weather.service.temp.TempTrendService;
import com.weather.service.wind.WindRankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final RainRankingService rainRankingService;
    private final TempRankingService tempRankingService;
    private final SunRankingService sunRankingService;
    private final HumRankingService humRankingService;
    private final WindRankingService windRankingService;
    private final RainTrendService rainTrendService;
    private final TempTrendService tempTrendService;

    @Cacheable(cacheNames = "weatherData", key = "'dashboardSummary'")
    public DashboardSummaryVO getSummary() {
        // 用聚合查询拿年份范围/城市数，不再拉全表计算
        Integer latestYear = rainRankingService.latestYear();
        if (latestYear == null) {
            return DashboardSummaryVO.builder()
                    .latestYear(null).cityCount(0).yearRange("")
                    .rain(List.of()).temp(List.of()).sun(List.of()).hum(List.of()).wind(List.of())
                    .rainTrend(List.of())
                    .tempTrend(List.of())
                    .build();
        }
        Integer minYear = rainRankingService.minYear();

        return DashboardSummaryVO.builder()
                .latestYear(latestYear)
                .cityCount((int) rainRankingService.countCities())
                .yearRange(minYear + "-" + latestYear)
                .rain(rainRankingService.listByYear(latestYear))
                .temp(tempRankingService.listByYear(latestYear))
                .sun(sunRankingService.listByYear(latestYear))
                .hum(humRankingService.listByYear(latestYear))
                .wind(windRankingService.listByYear(latestYear))
                .rainTrend(buildRainTrend(rainTrendService.listAll()))
                .tempTrend(buildTempTrend(tempTrendService.listAll()))
                .build();
    }

    private List<Map<String, Object>> buildRainTrend(List<RainTrend> trends) {
        return buildTrend(trends, RainTrend::getYear, RainTrend::getCity, RainTrend::getTotalRain);
    }

    private List<Map<String, Object>> buildTempTrend(List<TempTrend> trends) {
        return buildTrend(trends, TempTrend::getYear, TempTrend::getCity, TempTrend::getAvgTemp);
    }

    private <T> List<Map<String, Object>> buildTrend(List<T> trends,
                                                     Function<T, Integer> year,
                                                     Function<T, String> city,
                                                     Function<T, Object> value) {
        Map<Integer, Map<String, Object>> byYear = new TreeMap<>();
        for (T t : trends) {
            byYear.computeIfAbsent(year.apply(t), y -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("year", y);
                return row;
            }).put(city.apply(t), value.apply(t));
        }
        return new ArrayList<>(byYear.values());
    }
}
