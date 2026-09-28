package com.weather.dto.dashboard;

import com.weather.entity.hum.HumRanking;
import com.weather.entity.rain.RainRanking;
import com.weather.entity.sun.SunRanking;
import com.weather.entity.temp.TempRanking;
import com.weather.entity.wind.WindRanking;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class DashboardSummaryVO {

    private Integer latestYear;

    private Integer cityCount;

    private String yearRange;

    private List<RainRanking> rain;

    private List<TempRanking> temp;

    private List<SunRanking> sun;

    private List<HumRanking> hum;

    private List<WindRanking> wind;

    /**
     * 21年降水对比：[{year:2000, 泉州市:1711.1, 福州市:xxx}, ...]
     */
    private List<Map<String, Object>> rainTrend;

    /**
     * 21年平均气温对比：[{year:2000, 泉州市:20.1, 福州市:xxx}, ...]
     */
    private List<Map<String, Object>> tempTrend;
}
