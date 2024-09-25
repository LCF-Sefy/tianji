package com.tianji.stock.service;

import com.tianji.stock.domain.dto.CouponStockDTO;

public interface IStockService {

    /**
     * 商家设置优惠券库存
     * @param dto
     */
    void setCouponStock(CouponStockDTO dto);

    /**
     * 更新已领取数量+1（扣减库存）
     *
     * @param creater  （商家id，商家优惠券是按照商家进行分库分表，库存表按照优惠券id进行分库分表）
     * @param couponId 优惠券id
     */
    int increaseInssueNum(Long creater, Long couponId);


}
