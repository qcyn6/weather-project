package com.weather.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ADS 层数据为静态存量（2000-2020），查询结果全部进程内缓存，避免重复打库。
 * 数据经离线数仓更新后需重启服务（或接入 TTL）才能刷新。
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("weatherData");
    }
}
