package com.tianji.promotion.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.domain.dto.CouponFormDTO;
import com.tianji.promotion.domain.dto.CouponIssueFormDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.promotion.domain.query.CouponQuery;
import com.tianji.promotion.domain.vo.CouponPageVO;
import com.tianji.promotion.domain.vo.CouponVO;

import java.util.List;

/**
 * <p>
 * 优惠券的规则信息 服务类
 * </p>
 *
 * @author sefy
 * @since 2024-05-15
 */
public interface ICouponService extends IService<Coupon> {

    /**
     * 新增优惠券
     * @param dto
     */
    void saveCoupon(CouponFormDTO dto);

    /**
     * 分页查询优惠券列表-管理端
     * @param query
     * @return
     */
    PageDTO<CouponPageVO> queryCouponPage(CouponQuery query);

    /**
     * 发放优惠券
     * @param id
     * @param dto
     */
    void issueCoupon(Long id, CouponIssueFormDTO dto);

    /**
     * 查询发放中的优惠券
     * @return
     */
    List<CouponVO> queryIssuingCoupons();
}
