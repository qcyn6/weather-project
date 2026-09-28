package com.weather.service.temp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.weather.entity.temp.TempRanking;
import com.weather.mapper.temp.TempRankingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TempRankingService {

    private final TempRankingMapper tempRankingMapper;

    @Cacheable(cacheNames = "weatherData", key = "'tempRanking:' + (#year ?: 'all')")
    public List<TempRanking> listByYear(Integer year) {
        LambdaQueryWrapper<TempRanking> wrapper = new LambdaQueryWrapper<>();
        if (year != null) {
            wrapper.eq(TempRanking::getYear, year);
        }
        wrapper.orderByDesc(TempRanking::getYear)
                .orderByAsc(TempRanking::getRankNum);
        return tempRankingMapper.selectList(wrapper);
    }
}
