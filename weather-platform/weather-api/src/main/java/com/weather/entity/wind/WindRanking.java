package com.weather.entity.wind;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("ads_wind_ranking")
public class WindRanking {

    private Integer year;

    private Integer rankNum;

    private String city;

    private String province;

    private Double avgWind;

    private Double maxWind;

    private Integer galeDays;
}
