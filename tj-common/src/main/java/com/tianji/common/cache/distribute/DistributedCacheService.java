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
package com.tianji.common.cache.distribute;

import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 分布式缓存接口
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
public interface DistributedCacheService {

    void put(String key, String value);

    void put(String key, Object value);

    void put(String key, Object value, long timeout, TimeUnit unit);

    void put(String key, Object value, long expireTime);

    <T> T getObject(String key, Class<T> targetClass);

    Object getObject(String key);

    String getString(String key);

    <T> List<T> getList(String key, Class<T> targetClass);

    Boolean delete(String key);

    Boolean deleteKeyPrefix(String prefix);

    Boolean hasKey(String key);

    Long addSet(String key, Object... values);

    Long removeSet(String key, Object... values);

    Boolean isMemberSet(String key, Object o);

    void addHash(String key, Map<String, String> map);

    void addHash(String key, String hashKey, String score);

    Object getObject(String key, String hashKey);

//    /**
//     * 执行Lua脚本
//     */
//    Long execute(RedisScript<Long> script, List<String> keys, Object... args);

    /**
     * 扣减内存中的数据
     */
    default Long decrement(String key, long delta){
        return null;
    }
    /**
     * 增加内存中的数据
     */
    default Long increment(String key, long delta){
        return null;
    }

    /**
     * 使用Lua脚本扣减库存
     */
    default Long decrementByLua(String key, Integer quantity){
        return null;
    }
    /**
     * 使用Lua脚本增加库存
     */
    default Long incrementByLua(String key, Integer quantity){
        return null;
    }

    /**
     * 使用Lua脚本初始化库存
     */
    default Long initByLua(String key, Integer quantity){
        return null;
    }

    /**
     * 检测是否已经恢复缓存的库存数据
     */
    default Long checkExecute(String key, Long seconds){
        return null;
    }

    /**
     * 获取下单许可
     */
    default Long takeOrderToken(String key){
        return null;
    }
    /**
     * 恢复下单许可
     */
    default Long recoverOrderToken(String key){
        return null;
    }

    /**
     * 扣减分桶库存
     */
    default Long decrementBucketStock(List<String> keys, Integer quantity){
        return null;
    }

    /**
     * 恢复分桶库存
     */
    default Long incrementBucketStock(List<String> keys, Integer quantity){
        return null;
    }
}
