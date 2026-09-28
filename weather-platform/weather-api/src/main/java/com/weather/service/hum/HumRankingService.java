package com.weather.service.hum;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.weather.entity.hum.HumRanking;
import com.weather.mapper.hum.HumRankingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HumRankingService {

    private final HumRankingMapper humRankingMapper;

    @Cacheable(cacheNames = "weatherData", key = "'humRanking:' + (#year ?: 'all')")
    public List<HumRanking> listByYear(Integer year) {
        LambdaQueryWrapper<HumRanking> wrapper = new LambdaQueryWrapper<>();
        if (year != null) {
            wrapper.eq(HumRanking::getYear, year);
        }
        wrapper.orderByDesc(HumRanking::getYear)
                .orderByAsc(HumRanking::getRankNum);
        return humRankingMapper.selectList(wrapper);
    }
}
