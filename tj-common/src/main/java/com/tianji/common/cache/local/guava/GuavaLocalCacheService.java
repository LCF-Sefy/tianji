///**
// * Copyright 2022-9999 the original author or authors.
// * <p>
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// * <p>
// * http://www.apache.org/licenses/LICENSE-2.0
// * <p>
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package com.tianji.common.cache.local.guava;
//
//import com.google.common.cache.Cache;
//import com.tianji.common.cache.local.LocalCacheService;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
//import org.springframework.stereotype.Service;
//
///**
// * 基于Guava实现的本地缓存
// */
//@Service
//public class GuavaLocalCacheService<K, V> implements LocalCacheService<K, V> {
//    //本地缓存，基于Guava实现
//    private final Cache<K, V> cache = LocalCacheFactory.getGuavaLocalCache();
//
//    @Override
//    public void put(K key, V value) {
//        cache.put(key, value);
//    }
//
//    @Override
//    public V getIfPresent(Object key) {
//        return cache.getIfPresent(key);
//    }
//
//    @Override
//    public void delete(Object key) {
//        cache.invalidate(key);
//    }
//}
