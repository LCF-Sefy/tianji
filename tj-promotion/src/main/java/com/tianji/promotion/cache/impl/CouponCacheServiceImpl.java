package com.tianji.promotion.cache.impl;

import com.alibaba.fastjson.JSON;
import com.tianji.common.cache.distribute.DistributedCacheService;
import com.tianji.common.cache.local.LocalCacheService;
import com.tianji.common.domain.cache.CouponBusinessCache;
import com.tianji.common.lock.DistributedLock;
import com.tianji.common.lock.factoty.DistributedLockFactory;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.JsonUtils;
import com.tianji.promotion.cache.CouponCacheService;
import com.tianji.promotion.constants.PromotionConstants;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.vo.CouponCacheVO;
import com.tianji.promotion.domain.vo.CouponDetailVO;
import com.tianji.promotion.domain.vo.CouponVO;
import com.tianji.promotion.mapper.CouponMapper;
import com.tianji.promotion.utils.builder.CouponCacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.C;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.json.Json;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
public class CouponCacheServiceImpl implements CouponCacheService {

    @Autowired
    private CouponMapper couponMapper;
    @Autowired
    private LocalCacheService<String, CouponBusinessCache<CouponVO>> localCacheService;

    @Autowired
    private DistributedCacheService distributedCacheService;

    @Autowired
    private DistributedLockFactory distributedLockFactory;

    //本地锁
    private final Lock localCacheUpdatelock = new ReentrantLock();

    private final static Logger logger = LoggerFactory.getLogger(CouponCacheServiceImpl.class);

    /**
     * 获取缓存中店铺某个优惠券详情
     *
     * @param shopId
     * @param couponId
     * @return
     */
    @Override
    public CouponBusinessCache<CouponVO> getCachedCouponDetail(Long shopId, Long couponId) {
        //先从本地缓存中获取
        String localKey = String.format(PromotionConstants.COUPON_LOCAL_CACHE_KEY_FORMAT, shopId, couponId);
        CouponBusinessCache<CouponVO> couponCache = localCacheService.getIfPresent(localKey);
        //命中本地缓存直接返回
        if (couponCache != null) {
            logger.info("获取商家:{}的优惠券:{},命中本地缓存", shopId, couponId);
            return couponCache;
        }
        //本地缓存没有命中，从分布式缓存中获取
        logger.info("获取商家:{}的优惠券:{},未命中本地缓存", shopId, couponId);
        return getDistributedCache(shopId, couponId);
    }

    /**
     * 从分布式缓存中获取店铺某个优惠券详情缓存
     *
     * @param shopId
     * @param couponId
     * @return
     */
    @Override
    public CouponBusinessCache<CouponVO> getDistributedCache(Long shopId, Long couponId) {
        logger.info("获取商家:{}的优惠券:{}，尝试从分布式缓存中获取", shopId, couponId);
        String key = String.format(PromotionConstants.SHOP_COUPON_DETAIL_KEY_FORMAT, shopId, couponId);

        CouponBusinessCache<CouponVO> couponBusinessCache = CouponCacheBuilder.getCouponBusinessCache
                (distributedCacheService.getObject(key), CouponVO.class);
        //如果分布式缓存没有命中
        if (couponBusinessCache == null) {
            //尝试更新分布式缓存中的数据，使用分布式锁只有一个线程去执行更新
            couponBusinessCache = tryUpdateCouponDetailCacheByLock(shopId, couponId, true);
        }
        //获取到的数据不为空，需要更新本地缓存
        if (couponBusinessCache != null) {
            //获取本地锁
            if (localCacheUpdatelock.tryLock()) {
                try {
                    //更新本地缓存
                    String localKey = String.format(PromotionConstants.COUPON_LOCAL_CACHE_KEY_FORMAT, shopId, couponId);
                    localCacheService.put(localKey, couponBusinessCache);
                    logger.info("商家:{}的优惠券:{}，本地缓存已更新", shopId, couponId);
                } finally {
                    localCacheUpdatelock.unlock();
                }
            }
        }
        return couponBusinessCache;
    }

    /**
     * 更新店铺某个优惠券详情的分布式缓存
     *
     * @param shopId
     * @param couponId
     * @param doubleCheck
     * @return
     */
    @Override
    public CouponBusinessCache<CouponVO> tryUpdateCouponDetailCacheByLock(Long shopId, Long couponId, boolean doubleCheck) {
        logger.info("尝试更新商家:{}的优惠券:{}的分布式缓存", shopId, couponId);
        //尝试更新分布式缓存中的数据，使用分布式锁只有一个线程去执行更新
        String distributedLockKey = String.format(PromotionConstants.COUPON_CACHE_UPDATE_LOCK_KEY_FORMAT, shopId, couponId);
        //获取分布式锁
        DistributedLock distributedLock = distributedLockFactory.getDistributedLock(distributedLockKey);
        String key = String.format(PromotionConstants.SHOP_COUPON_DETAIL_KEY_FORMAT, shopId, couponId);
        try {
            boolean isSuccess = distributedLock.tryLock(1, 5, TimeUnit.SECONDS);
            //获取分布式锁失败，则快速返回
            if (!isSuccess) {
                return new CouponBusinessCache<CouponVO>().retryLater();
            }
            CouponBusinessCache<CouponVO> couponBusinessCache = null;
            //如果开启了二次校验
            if (doubleCheck) {
                //获取分布式锁成功后，再次从缓存中获取数据，防止高并发下多个线程争抢过程中
                //后序的线程在等待一秒钟的过程中，已获取到分布式锁的线程释放了锁，防止后续线程获取到锁后再次更新缓存
                couponBusinessCache = CouponCacheBuilder.getCouponBusinessCache(distributedCacheService.getObject(key), CouponVO.class);
                //如果发现已经更新了，则直接返回
                if (couponBusinessCache != null) {
                    logger.info("已经有线程更新了分布式缓存，直接返回");
                    return couponBusinessCache;
                }
            }
            //从数据库中获取优惠券信息
            Coupon coupon = couponMapper.getByShopIdAndId(shopId, couponId);
            if (coupon == null) {
                //数据库中不存在
                couponBusinessCache = new CouponBusinessCache<CouponVO>().notExist();
            } else {
                couponBusinessCache = new CouponBusinessCache<CouponVO>().with(BeanUtils.copyBean(coupon, CouponVO.class));
            }
            //将从数据库中查询到的数据更新到分布式缓存（如果数据库中不存在，会缓存一个没有数据的对象，解决缓存穿透）
            distributedCacheService.put(key, couponBusinessCache, PromotionConstants.FIVE_MINUTES);
            logger.info("商家:{}的优惠券:{}的分布式缓存已经更新", shopId, couponId);
            return couponBusinessCache;
        } catch (InterruptedException e) {
            logger.error("商家:{}的优惠券:{}的分布式缓存更新失败", shopId, couponId);
            return new CouponBusinessCache<CouponVO>().retryLater();
        } finally {
            //释放分布式锁
            distributedLock.unlock();
        }
    }
}
