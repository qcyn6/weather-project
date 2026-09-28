package com.weather.service.wind;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.weather.entity.wind.WindRanking;
import com.weather.mapper.wind.WindRankingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WindRankingService {

    private final WindRankingMapper windRankingMapper;

    @Cacheable(cacheNames = "weatherData", key = "'windRanking:' + (#year ?: 'all')")
    public List<WindRanking> listByYear(Integer year) {
        LambdaQueryWrapper<WindRanking> wrapper = new LambdaQueryWrapper<>();
        if (year != null) {
            wrapper.eq(WindRanking::getYear, year);
        }
        wrapper.orderByDesc(WindRanking::getYear)
                .orderByAsc(WindRanking::getRankNum);
        return windRankingMapper.selectList(wrapper);
    }
}
