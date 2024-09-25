package com.tianji.promotion.dubbo.client;

import com.tianji.api.interfaces.stock.StockDubboService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

@Service
public class CouponStockClient {

    @DubboReference(version = "1.0.0")
    private StockDubboService stockDubboService;

    /**
     * 初始化优惠券库存（insert）
     *
     * @param couponId
     * @param stock
     * @param userId
     */
    public void setCouponStock(Long couponId, Integer stock, Long userId) {
        stockDubboService.setCouponStock(couponId, stock, userId);
    }

    /**
     * 更新已领取数量+1（扣减库存）
     *
     * @param couponId
     * @param creater
     */
    public int increaseInssueNum(Long creater, Long couponId) {
        return stockDubboService.increaseInssueNum(creater, couponId);
    }
}
