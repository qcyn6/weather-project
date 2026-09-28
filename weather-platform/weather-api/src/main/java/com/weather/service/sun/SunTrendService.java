package com.weather.service.sun;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.weather.entity.sun.SunTrend;
import com.weather.mapper.sun.SunTrendMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SunTrendService {

    private final SunTrendMapper sunTrendMapper;

    @Cacheable(cacheNames = "weatherData", key = "'sunTrend:' + #city")
    public List<SunTrend> listByCity(String city) {
        return sunTrendMapper.selectList(new LambdaQueryWrapper<SunTrend>()
                .eq(SunTrend::getCity, city)
                .orderByAsc(SunTrend::getYear));
    }
}
