package com.weather.service.hum;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.weather.entity.hum.HumTrend;
import com.weather.mapper.hum.HumTrendMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HumTrendService {

    private final HumTrendMapper humTrendMapper;

    @Cacheable(cacheNames = "weatherData", key = "'humTrend:' + #city")
    public List<HumTrend> listByCity(String city) {
        return humTrendMapper.selectList(new LambdaQueryWrapper<HumTrend>()
                .eq(HumTrend::getCity, city)
                .orderByAsc(HumTrend::getYear));
    }
}
