package com.tianji.promotion.mapper;

import com.tianji.promotion.domain.po.Coupon;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianji.promotion.domain.vo.CouponVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

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
    @Update("update coupon set issue_num = issue_num + 1 where creater = #{creater} and id = #{id}")
    int incrIssNum(@Param("creater") Long creater, @Param("id") Long id);

    @Select("select * from coupon where creater = #{creater} and id = #{id}")
    Coupon selectByCreaterAndIdCoupon(@Param("creater") Long creater, @Param("id") Long id);

    Coupon getByShopIdAndId(@Param("shopId") Long shopId, @Param("id") Long id);

    List<Coupon> getCouponListByShopIdAndStatus(@Param("shopId") Long shopId, @Param("status") Integer status);
}
