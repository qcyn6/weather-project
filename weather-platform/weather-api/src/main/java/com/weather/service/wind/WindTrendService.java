package com.weather.service.wind;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.weather.entity.wind.WindTrend;
import com.weather.mapper.wind.WindTrendMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WindTrendService {

    private final WindTrendMapper windTrendMapper;

    @Cacheable(cacheNames = "weatherData", key = "'windTrend:' + #city")
    public List<WindTrend> listByCity(String city) {
        return windTrendMapper.selectList(new LambdaQueryWrapper<WindTrend>()
                .eq(WindTrend::getCity, city)
                .orderByAsc(WindTrend::getYear));
    }
}
