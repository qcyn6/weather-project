package com.weather.entity.sun;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("ads_sun_ranking")
public class SunRanking {

    private Integer year;

    private Integer rankNum;

    private String city;

    private String province;

    private Double totalSun;

    private Double avgSun;

    private Integer sunnyDays;
}
