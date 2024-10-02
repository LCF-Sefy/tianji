package com.tianji.promotion.cache.impl;

import com.jd.platform.hotkey.client.callback.JdHotKeyStore;
import com.tianji.common.cache.distribute.DistributedCacheService;
import com.tianji.common.cache.local.LocalCacheService;
import com.tianji.common.domain.cache.CouponBusinessCache;
import com.tianji.common.lock.DistributedLock;
import com.tianji.common.lock.factoty.DistributedLockFactory;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.promotion.cache.CouponListCacheService;
import com.tianji.promotion.constants.PromotionConstants;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.vo.CouponVO;
import com.tianji.promotion.mapper.CouponMapper;
import com.tianji.promotion.utils.builder.CouponCacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class CouponListCacheHotKeyServiceImpl implements CouponListCacheService {

    @Autowired
    private CouponMapper couponMapper;
    @Autowired
    private LocalCacheService<String, CouponBusinessCache<List<CouponVO>>> localCacheService;

    @Autowired
    private DistributedCacheService distributedCacheService;

    @Autowired
    private DistributedLockFactory distributedLockFactory;

    //本地锁
    private final Lock localCacheUpdatelock = new ReentrantLock();

    private final static Logger logger = LoggerFactory.getLogger(CouponCacheHotKeyServiceImpl.class);

    /**
     * 获取缓存中店铺发放中的优惠券列表
     *
     * @param shopId
     * @param status
     * @return
     */
    @Override
    public CouponBusinessCache<List<CouponVO>> getCachedCouponList(Long shopId, Integer status) {
        //先判断是否是hotkey
        String localKey = String.format(PromotionConstants.SHOP_COUPON_HK_LIST_LOCAL_CACHE_KEY_FORMAT, shopId, status);
        boolean isHot = JdHotKeyStore.isHotKey(localKey);
        //如果是hotkey则尝试从本地缓存去
        if (isHot) {
            logger.info("{}:是hotkey", localKey);
            CouponBusinessCache<List<CouponVO>> couponCache = CouponCacheBuilder.getCouponBusinessCacheList(JdHotKeyStore.get(localKey), CouponVO.class);
            //是hot但是本地value为null，则需要从redis缓存中取出数据并更新
            if (couponCache == null) {
                logger.info("{}：是hotkey，但本地缓存为空，需要从redis中获取并更新本地缓存", localKey);
                return getDistributedCache(shopId, status, true);
            }
            return couponCache;
        }
        //如果不是hotkey，则尝试从分布式缓存中获取，并且无序更新hotkey本地缓存
        logger.info("{}：不是hotkey，需要从redis中获取", localKey);
        return getDistributedCache(shopId, status, false);
    }

    /**
     * 引入hotkey：从分布式缓存中获取店铺某个优惠券详情缓存
     *
     * @param shopId
     * @param status
     * @param isUpdate 是否更新hotkey本地缓存
     * @return
     */
    @Override
    public CouponBusinessCache<List<CouponVO>> getDistributedCache(Long shopId, Integer status, boolean isUpdate) {
        logger.info("从分布式缓存中获取数据");
        //分布式缓存key ： prs:list:coupon:商家id:优惠券状态
        String key = String.format(PromotionConstants.SHOP_COUPON_LIST_KEY_FORMAT, shopId, status);
        CouponBusinessCache<List<CouponVO>> couponListCache = CouponCacheBuilder.getCouponBusinessCacheList(distributedCacheService.getObject(key), CouponVO.class);
        //如果没有命中分布式缓存
        if (couponListCache == null) {
            //尝试更新分布式缓存中的数据，使用分布式锁只有一个线程去执行更新
            couponListCache = tryUpdateCouponListCacheByLock(shopId, status, true);
        }
        //如果获取到的数据不为空，需要更新本地缓存
        if (isUpdate && couponListCache != null) {
            //获取本地锁
            if (localCacheUpdatelock.tryLock()) {
                try {
                    //获取成功则更新本地缓存
                    String localKey = String.format(PromotionConstants.SHOP_COUPON_HK_LIST_LOCAL_CACHE_KEY_FORMAT, shopId, status);
                    JdHotKeyStore.smartSet(localKey,couponListCache);
                    logger.info("商家:{}状态为:{}的优惠券列表,本地缓存已经更新", shopId, status);
                } finally {
                    localCacheUpdatelock.unlock();
                }
            }
        }
        return couponListCache;
    }

    /**
     * 从分布式缓存中获取店铺某个状态的优惠券列表
     *
     * @param shopId
     * @param status
     * @return
     */
    @Override
    public CouponBusinessCache<List<CouponVO>> getDistributedCache(Long shopId, Integer status) {
        logger.info("获取商家:{}状态为:{}的优惠券列表,尝试从分布式缓存中获取", shopId, status);
        //分布式缓存key ： prs:list:coupon:商家id:优惠券状态
        String key = String.format(PromotionConstants.SHOP_COUPON_LIST_KEY_FORMAT, shopId, status);
        CouponBusinessCache<List<CouponVO>> couponListCache = CouponCacheBuilder.getCouponBusinessCacheList(distributedCacheService.getObject(key), CouponVO.class);
        //如果没有命中分布式缓存
        if (couponListCache == null) {
            //尝试更新分布式缓存中的数据，使用分布式锁只有一个线程去执行更新
            couponListCache = tryUpdateCouponListCacheByLock(shopId, status, true);
        }
        //如果获取到的数据不为空，需要更新本地缓存
        if (couponListCache != null) {
            //获取本地锁
            if (localCacheUpdatelock.tryLock()) {
                try {
                    //获取成功则更新本地缓存
                    String localKey = String.format(PromotionConstants.SHOP_COUPON_LIST_LOCAL_CACHE_KEY_FORMAT, shopId, status);
                    localCacheService.put(localKey, couponListCache);
                    logger.info("商家:{}状态为:{}的优惠券列表,本地缓存已经更新", shopId, status);
                } finally {
                    localCacheUpdatelock.unlock();
                }
            }
        }
        return couponListCache;
    }


    /**
     * 更新店铺优惠券列表缓存
     *
     * @param shopId
     * @param status
     * @param doubleCheck
     * @return
     */
    @Override
    public CouponBusinessCache<List<CouponVO>> tryUpdateCouponListCacheByLock(Long shopId, Integer status, boolean doubleCheck) {
        logger.info("尝试更新商家:{}状态为:{}的优惠券列表的分布式缓存", shopId, status);
        //尝试更新分布式缓存中的数据，使用分布式锁只有一个线程去执行更新
        String distributedLockKey = String.format(PromotionConstants.SHOP_COUPON_LIST_CACHE_UPDATE_LOCK_KEY_FORMAT, shopId, status);
        //获取分布式锁
        DistributedLock distributedLock = distributedLockFactory.getDistributedLock(distributedLockKey);
        String key = String.format(PromotionConstants.SHOP_COUPON_LIST_KEY_FORMAT, shopId, status);
        try {
            boolean isSuccess = distributedLock.tryLock();
            //未获取到分布式锁，则快速返回稍后重试
            if (!isSuccess) {
                return new CouponBusinessCache<List<CouponVO>>().retryLater();
            }
            CouponBusinessCache<List<CouponVO>> couponListCache = null;
            //如果开启了二次校验
            if (doubleCheck) {
                //获取分布式锁成功后，再次从缓存中获取数据，防止高并发下多个线程争抢过程中
                //后序的线程在等待一秒钟的过程中，已获取到分布式锁的线程释放了锁，防止后续线程获取到锁后再次更新缓存
                couponListCache = CouponCacheBuilder.getCouponBusinessCacheList(distributedCacheService.getObject(key), CouponVO.class);
                //如果发现已经更新了，则直接返回
                if (couponListCache != null) {
                    logger.info("已经有线程更新了分布式缓存，直接返回");
                    return couponListCache;
                }
            }
            //从数据库中获取优惠券列表信息 条件：shopId、status
            List<Coupon> couponList = couponMapper.getCouponListByShopIdAndStatus(shopId, status);
            if (CollUtils.isEmpty(couponList)) {
                //数据库中不存在，直接返回不存在
                couponListCache = new CouponBusinessCache<List<CouponVO>>().notExist();
                return couponListCache;
            } else {
                couponListCache = new CouponBusinessCache<List<CouponVO>>().with(BeanUtils.copyList(couponList, CouponVO.class));
            }
            //将从数据库中查询到的数据更新到分布式缓存
            distributedCacheService.put(key, couponListCache, PromotionConstants.FIVE_MINUTES);
            logger.info("商家:{}状态为:{}的优惠券列表的分布式缓存已经更新", shopId, status);
            return couponListCache;
        } catch (InterruptedException e) {
            logger.error("更新商家:{}状态为:{}的优惠券列表的分布式缓存失败", shopId, status, e);
            return new CouponBusinessCache<List<CouponVO>>().retryLater();
        } finally {
            //释放分布式锁
            distributedLock.unlock();
        }
    }
}
