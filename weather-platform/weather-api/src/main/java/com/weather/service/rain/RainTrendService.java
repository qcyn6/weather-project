package com.weather.service.rain;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.weather.entity.rain.RainTrend;
import com.weather.mapper.rain.RainTrendMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RainTrendService {

    private final RainTrendMapper rainTrendMapper;

    @Cacheable(cacheNames = "weatherData", key = "'rainTrend:' + #city")
    public List<RainTrend> listByCity(String city) {
        return rainTrendMapper.selectList(new LambdaQueryWrapper<RainTrend>()
                .eq(RainTrend::getCity, city)
                .orderByAsc(RainTrend::getYear));
    }

    @Cacheable(cacheNames = "weatherData", key = "'rainTrendAll'")
    public List<RainTrend> listAll() {
        return rainTrendMapper.selectList(new LambdaQueryWrapper<RainTrend>()
                .orderByAsc(RainTrend::getYear));
    }
}
