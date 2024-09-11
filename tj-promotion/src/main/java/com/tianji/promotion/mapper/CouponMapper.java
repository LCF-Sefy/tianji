package com.tianji.promotion.mapper;

import com.tianji.promotion.domain.po.Coupon;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * <p>
 * 优惠券的规则信息 Mapper 接口
 * </p>
 *
 * @author sefy
 * @since 2024-05-15
 */
public interface CouponMapper extends BaseMapper<Coupon> {

    /**
     * 更新优惠券已领取数量
     *
     * @param id
     */
    @Update("update coupon set issue_num = issue_num + 1 where id = #{id}")
    int incrIssNum(@Param("id") Long id);
}