package com.weather.entity.temp;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("ads_temp_ranking")
public class TempRanking {

    private Integer year;

    private Integer rankNum;

    private String city;

    private String province;

    private Double avgTemp;

    private Double maxTemp;

    private Double minTemp;
}
