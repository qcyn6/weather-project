package com.weather.entity.hum;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("ads_hum_ranking")
public class HumRanking {

    private Integer year;

    private Integer rankNum;

    private String city;

    private String province;

    private Double avgHumidity;

    private Double maxHumidity;

    private Double minHumidity;
}
