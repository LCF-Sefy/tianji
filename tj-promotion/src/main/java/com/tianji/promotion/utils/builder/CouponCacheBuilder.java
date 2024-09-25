package com.tianji.promotion.utils.builder;

import com.alibaba.druid.support.json.JSONUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.tianji.common.domain.cache.CouponBusinessCache;

import java.util.List;


public class CouponCacheBuilder {

    /**
     * Json泛型化处理
     */
    public static <P, T> CouponBusinessCache<T> getCouponBusinessCache(Object object, Class<T> clazz) {
        if (object == null) {
            return null;
        }
        return JSON.parseObject(JSON.toJSONString(object), new TypeReference<CouponBusinessCache<T>>(clazz) {
        });
    }

    /**
     * Json泛型化处理
     */
    public static <T> CouponBusinessCache<List<T>> getCouponBusinessCacheList(Object object, Class<T> clazz) {
        if (object == null) {
            return null;
        }
        return JSON.parseObject(JSON.toJSONString(object), new TypeReference<CouponBusinessCache<List<T>>>(clazz) {
        });
    }
}
