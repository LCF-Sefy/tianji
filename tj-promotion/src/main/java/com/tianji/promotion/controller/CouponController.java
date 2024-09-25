package com.tianji.promotion.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.promotion.domain.dto.CouponFormDTO;
import com.tianji.promotion.domain.dto.CouponIssueFormDTO;
import com.tianji.promotion.domain.po.CouponScope;
import com.tianji.promotion.domain.query.CouponQuery;
import com.tianji.promotion.domain.vo.CouponPageVO;
import com.tianji.promotion.domain.vo.CouponVO;
import com.tianji.promotion.service.ICouponService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * <p>
 * 优惠券的规则信息 前端控制器
 * </p>
 *
 * @author sefy
 * @since 2024-05-15
 */
@Api(tags = "优惠券相关接口")
@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final ICouponService couponService;

    @ApiOperation("新增优惠券-管理端")
    @PostMapping
    public void saveCoupon(@RequestBody @Validated CouponFormDTO dto) {
        couponService.saveCoupon(dto);
    }

    @ApiOperation("分页查询优惠券列表-管理端")
    @GetMapping("page")
    public PageDTO<CouponPageVO> queryCouponPage(CouponQuery query) {
        return couponService.queryCouponPage(query);
    }

    @ApiOperation("发放优惠券")
    @PutMapping("{id}/issue")
    public void issueCoupon(@PathVariable(value = "id") Long id, @RequestBody @Validated CouponIssueFormDTO dto) {
        couponService.issueCoupon(id, dto);
    }

    @ApiOperation("查询发放中的优惠券")
    @GetMapping("list")
    public List<CouponVO> queXryIssuingCoupons(){
        return couponService.queryIssuingCoupons();
    }


    /**
     * 根据优惠券状态查询商家优惠券列表
     * @param shopId
     * @return
     */
    @ApiOperation("查询商家发放中的优惠券")
    @GetMapping("list/{shopId}/{status}")
    public List<CouponVO> list(@PathVariable("shopId") Long shopId ,@PathVariable("status") Integer status){
        return couponService.getCouponList(shopId, status);
    }

    @ApiOperation("查询优惠券详情")
    @GetMapping("detail/{shopId}/{couponId}")
    public CouponVO detail(@PathVariable("shopId") Long shopId, @PathVariable("couponId") Long couponId){
        return couponService.detail(shopId, couponId);
    }
}
