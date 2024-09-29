package com.tianji.common.domain.mq;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户所领券的优惠券信息
 */
@Data
public class CouponMessage implements Serializable {

    /**
     * 商家id
     */
    private Long creater;

    /**
     * 用户id
     */
    private Long userId;
    /**
     * 优惠券id
     */
    private Long couponId;

    /**
     * 兑换码序列号id
     */
    private Integer exchangeCodeId;
}
