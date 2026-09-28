package com.weather.entity.wind;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("ads_wind_trend")
public class WindTrend {

    private String city;

    private Integer year;

    private Double avgWind;

    private Double maxWind;

    private Integer galeDays;

    private String trendLabel;
}
