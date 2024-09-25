package com.tianji.common.autoconfigure.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.tianji.common.cache.local.guava.CaffeineLocalCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class LocalCacheConfig<K, V> {

    @Bean
    @ConditionalOnMissingBean
    public CaffeineLocalCacheService<K, V> caffeineLocalCacheService() {
        return new CaffeineLocalCacheService<K, V>();
    }

}
