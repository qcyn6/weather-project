package com.weather.entity.rain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("ads_rain_trend")
public class RainTrend {

    private String city;

    private Integer year;

    private Double totalRain;

    private Integer rainyDays;

    private String trendLabel;
}
