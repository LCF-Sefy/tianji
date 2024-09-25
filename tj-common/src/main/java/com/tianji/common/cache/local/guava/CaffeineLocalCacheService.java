package com.tianji.common.cache.local.guava;

import com.github.benmanes.caffeine.cache.Cache;
import com.tianji.common.cache.local.LocalCacheService;
import com.tianji.common.domain.CommonCache;
import com.tianji.common.domain.cache.CouponBusinessCache;
import org.springframework.stereotype.Service;


/**
 * 基于Caffeine实现的本地缓存
 */
@Service
public class CaffeineLocalCacheService<K, V> implements LocalCacheService<K, V> {

    public final Cache<K, V> cache = LocalCacheFactory.getCaffeineLocalCache();

    @Override
    public void put(K key, V value) {
        cache.put(key, value);
    }

    @Override
    public V getIfPresent(K key) {
        return cache.getIfPresent(key);
    }

    @Override
    public void delete(Object key) {
        cache.invalidate(key);
    }
}
