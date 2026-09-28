package com.weather.entity.hum;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("ads_hum_trend")
public class HumTrend {

    private String city;

    private Integer year;

    private Double avgHumidity;

    private Integer highHumDays;

    private String trendLabel;
}
