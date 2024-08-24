package com.tianji.promotion.service;

import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 兑换码 服务类
 * </p>
 *
 * @author sefy
 * @since 2024-05-15
 */
public interface IExchangeCodeService extends IService<ExchangeCode> {

    /**
     * 异步生成兑换码
     * @param coupon
     */
    void asycGenerateExchangeCode(Coupon coupon);

    /**
     * 修改兑换码状态：是否领取的状态 操作redis的bitmap
     * @param couponId
     * @param flag
     * @return
     */
    boolean updateExchangeCodeMark(long couponId, boolean flag);

    Long exchangeTargetId(long exchangeCodeId);
}
