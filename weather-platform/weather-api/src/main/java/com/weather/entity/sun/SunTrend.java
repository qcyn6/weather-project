package com.weather.entity.sun;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("ads_sun_trend")
public class SunTrend {

    private String city;

    private Integer year;

    private Double totalSun;

    private Double avgSun;

    private String trendLabel;
}
