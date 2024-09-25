package com.tianji.promotion.service;

import com.tianji.api.dto.user.UserDTO;
import com.tianji.promotion.domain.dto.CouponDiscountDTO;
import com.tianji.promotion.domain.dto.OrderCourseDTO;
import com.tianji.promotion.domain.dto.UserCouponDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.UserCoupon;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 用户领取优惠券的记录，是真正使用的优惠券信息 服务类
 * </p>
 *
 * @author sefy
 * @since 2024-05-18
 */
public interface IUserCouponService extends IService<UserCoupon> {

    /**
     * 领取优惠券
     *
     * @param id
     */
    void receiveCoupon(Long id);

    /**
     * 兑换码兑换优惠券
     *
     * @param code
     */
    void exchangeCoupon(String code);

//    /**
//     * 校验是否超出优惠券的每个人限领取数量 和 优惠券的已发放数量 + 1 和 生成用户券
//     * @param userId
//     * @param coupon
//     * @param serialNum
//     */
//    public void checkAndCreateUserCoupon(Long userId, Coupon coupon, Long serialNum);

    void checkAndCreateUserCouponNew(UserCouponDTO msg);

    /**
     * 查询我的可用优惠券方案
     * @param orderCourses
     * @return
     */
    List<CouponDiscountDTO> findDiscountSolution(List<OrderCourseDTO> orderCourses);

    /**
     * 根据shopId 和 id领取优惠券
     * @param shopId  商家id（creater）
     * @param id      优惠券id
     */
    void receiveCouponByShopIdAndId(Long shopId, Long id);
}
