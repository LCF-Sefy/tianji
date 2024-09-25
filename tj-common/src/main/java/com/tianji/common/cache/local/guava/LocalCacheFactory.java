/**
 * Copyright 2022-9999 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tianji.common.cache.local.guava;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.concurrent.TimeUnit;

/**
 * 本地缓存工厂
 */
public class LocalCacheFactory {

    /**
     * 缓存的最大容量
     */
    private static final int MAXIMUM_SIZE = 100;

    /**
     * 缓存项的写入后过期时间
     */
    private static final Long EXPIRE_AFTER_WRITE_DURATION = 5L;

    public static <K, V> Cache<K, V> getGuavaLocalCache() {
        return CacheBuilder.newBuilder().initialCapacity(15).concurrencyLevel(5).expireAfterWrite(10, TimeUnit.SECONDS).build();
    }

    public static <K, V> com.github.benmanes.caffeine.cache.Cache<K, V> getCaffeineLocalCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(EXPIRE_AFTER_WRITE_DURATION, TimeUnit.SECONDS)
//                .expireAfterAccess(1, TimeUnit.SECONDS)
                .maximumSize(MAXIMUM_SIZE)
                .build();
    }

}
