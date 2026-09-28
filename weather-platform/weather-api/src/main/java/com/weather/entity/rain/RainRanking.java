package com.weather.entity.rain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("ads_rain_ranking")
public class RainRanking {

    private Integer year;

    private Integer rankNum;

    private String city;

    private String province;

    private Double totalRain;

    private Integer rainyDays;
}
