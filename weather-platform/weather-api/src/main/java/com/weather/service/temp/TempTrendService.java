package com.weather.service.temp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.weather.entity.temp.TempTrend;
import com.weather.mapper.temp.TempTrendMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TempTrendService {

    private final TempTrendMapper tempTrendMapper;

    @Cacheable(cacheNames = "weatherData", key = "'tempTrend:' + #city")
    public List<TempTrend> listByCity(String city) {
        return tempTrendMapper.selectList(new LambdaQueryWrapper<TempTrend>()
                .eq(TempTrend::getCity, city)
                .orderByAsc(TempTrend::getYear));
    }

    @Cacheable(cacheNames = "weatherData", key = "'tempTrendAll'")
    public List<TempTrend> listAll() {
        return tempTrendMapper.selectList(new LambdaQueryWrapper<TempTrend>()
                .orderByAsc(TempTrend::getYear));
    }
}
