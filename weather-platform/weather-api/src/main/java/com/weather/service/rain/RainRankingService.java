package com.weather.service.rain;

import cn.hutool.core.convert.Convert;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.weather.entity.rain.RainRanking;
import com.weather.mapper.rain.RainRankingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RainRankingService {

    private final RainRankingMapper rainRankingMapper;

    @Cacheable(cacheNames = "weatherData", key = "'rainRanking:' + (#year ?: 'all')")
    public List<RainRanking> listByYear(Integer year) {
        LambdaQueryWrapper<RainRanking> wrapper = new LambdaQueryWrapper<>();
        if (year != null) {
            wrapper.eq(RainRanking::getYear, year);
        }
        wrapper.orderByDesc(RainRanking::getYear)
                .orderByAsc(RainRanking::getRankNum);
        return rainRankingMapper.selectList(wrapper);
    }

    @Cacheable(cacheNames = "weatherData", key = "'cities'")
    public List<String> listCities() {
        return rainRankingMapper.selectObjs(new QueryWrapper<RainRanking>()
                        .select("DISTINCT city"))
                .stream()
                .map(String::valueOf)
                .sorted()
                .toList();
    }

    public Integer latestYear() {
        return singleInt("MAX(year)");
    }

    public Integer minYear() {
        return singleInt("MIN(year)");
    }

    public long countCities() {
        return Convert.toLong(
                rainRankingMapper.selectObjs(new QueryWrapper<RainRanking>()
                        .select("COUNT(DISTINCT city)")).stream().findFirst().orElse(0), 0L);
    }

    private Integer singleInt(String aggregate) {
        return rainRankingMapper.selectObjs(new QueryWrapper<RainRanking>().select(aggregate))
                .stream()
                .findFirst()
                .map(v -> Convert.toInt(v, null))
                .orElse(null);
    }
}
