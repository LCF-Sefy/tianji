package com.tianji.api.interfaces.stock;

public interface StockDubboService {

    /**
     * 设置商品库存
     * @param couponId
     * @param stock
     */
    void setCouponStock(Long couponId, Integer stock, Long userId);

    /**
     * 更新已领取数量+1（扣减库存）
     *
     * @param creater（商家id，商家优惠券是按照商家进行分库分表，库存表按照优惠券id进行分库分表）
     * @param couponId                                        优惠券id
     * @return
     */
    int increaseInssueNum(Long creater, Long couponId);
}
