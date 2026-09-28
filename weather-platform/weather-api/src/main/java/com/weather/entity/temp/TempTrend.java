package com.weather.entity.temp;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("ads_temp_trend")
public class TempTrend {

    private String city;

    private Integer year;

    private Double avgTemp;

    private Double maxTemp;

    private Double minTemp;

    private String trendLabel;
}
