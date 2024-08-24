package com.tianji.promotion.mapper;

import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.UserCoupon;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 用户领取优惠券的记录，是真正使用的优惠券信息 Mapper 接口
 * </p>
 *
 * @author sefy
 * @since 2024-05-18
 */
public interface UserCouponMapper extends BaseMapper<UserCoupon> {

    /**
     * 查询当前用户可用的优惠券 coupon表 和 user_coupon表  条件：userId status=1 查哪些字段 优惠券的规则 优惠券id 用户券id
     *
     * @param userId
     * @return
     */
    @Select("select c.id, c.discount_type, c.`specific`, c.threshold_amount, c.discount_value, c.max_discount_amount, uc.id as creater \n" +
            "from " +
            "coupon c INNER join user_coupon uc on c.id = uc.coupon_id " +
            "where uc.user_id = #{userId} and uc.`status` = 1")
    List<Coupon> queryMyCoupon(Long userId);
}
