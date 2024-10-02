package com.tianji.promotion.cache;

import com.tianji.common.domain.cache.CouponBusinessCache;
import com.tianji.promotion.domain.vo.CouponVO;

import java.util.List;

public interface CouponCacheService {

    /**
     * 获取缓存中店铺某个优惠券详情
     *
     * @param shopId
     * @param couponId
     * @return
     */
    CouponBusinessCache<CouponVO> getCachedCouponDetail(Long shopId, Long couponId);

    /**
     * 引入hotkey：从分布式缓存中获取店铺某个优惠券详情缓存
     * @param shopId
     * @param couponId
     * @param isUpdate 是否更新hotkey本地缓存
     * @return
     */
    CouponBusinessCache<CouponVO> getDistributedCache(Long shopId, Long couponId, boolean isUpdate);

    /**
     * 更新店铺某个优惠券详情缓存
     *
     * @param shopId
     * @return
     */
    CouponBusinessCache<CouponVO> tryUpdateCouponDetailCacheByLock(Long shopId, Long couponId, boolean doubleCheck);

    /**
     * 从分布式缓存中获取店铺某个优惠券详情缓存
     * @param shopId
     * @param couponId
     * @return
     */
    CouponBusinessCache<CouponVO> getDistributedCache(Long shopId, Long couponId);


}
