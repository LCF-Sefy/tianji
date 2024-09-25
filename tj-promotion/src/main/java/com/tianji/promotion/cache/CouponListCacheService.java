package com.tianji.promotion.cache;

import com.tianji.common.domain.cache.CouponBusinessCache;
import com.tianji.promotion.domain.vo.CouponVO;

import java.util.List;

public interface CouponListCacheService {

    /**
     * 获取缓存中店铺优惠券列表
     *
     * @param shopId
     * @return
     */
    CouponBusinessCache<List<CouponVO>> getCachedCouponList(Long shopId, Integer status);

    /**
     * 更新店铺优惠券列表缓存
     *
     * @param shopId
     * @param status
     * @param doubleCheck
     * @return
     */
    CouponBusinessCache<List<CouponVO>> tryUpdateCouponListCacheByLock(Long shopId, Integer status, boolean doubleCheck);


    /**
     * 从分布式缓存中获取店铺某个状态的优惠券列表
     *
     * @param shopId
     * @param status
     * @return
     */
    CouponBusinessCache<List<CouponVO>> getDistributedCache(Long shopId, Integer status);

}
