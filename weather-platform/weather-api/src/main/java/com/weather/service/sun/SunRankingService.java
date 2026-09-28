package com.weather.service.sun;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.weather.entity.sun.SunRanking;
import com.weather.mapper.sun.SunRankingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SunRankingService {

    private final SunRankingMapper sunRankingMapper;

    @Cacheable(cacheNames = "weatherData", key = "'sunRanking:' + (#year ?: 'all')")
    public List<SunRanking> listByYear(Integer year) {
        LambdaQueryWrapper<SunRanking> wrapper = new LambdaQueryWrapper<>();
        if (year != null) {
            wrapper.eq(SunRanking::getYear, year);
        }
        wrapper.orderByDesc(SunRanking::getYear)
                .orderByAsc(SunRanking::getRankNum);
        return sunRankingMapper.selectList(wrapper);
    }
}
