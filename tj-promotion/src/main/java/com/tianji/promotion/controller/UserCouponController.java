package com.tianji.promotion.controller;


import com.tianji.promotion.domain.dto.CouponDiscountDTO;
import com.tianji.promotion.domain.dto.OrderCourseDTO;
import com.tianji.promotion.service.IUserCouponService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 用户领取优惠券的记录，是真正使用的优惠券信息 前端控制器
 * </p>
 *
 * @author sefy
 * @since 2024-05-18
 */
@Api(tags = "用户券相关接口")
@RestController
@RequestMapping("/user-coupons")
@RequiredArgsConstructor
public class UserCouponController {

    private final IUserCouponService userCouponService;

    @ApiOperation("领取优惠劵")
    @PostMapping("{id}/receive")
    public void receiveCoupon(@PathVariable Long id) {
        userCouponService.receiveCoupon(id);
    }

    @ApiOperation("兑换码兑换优惠券")
    @PostMapping("/{code}/exchange")
    public void exchangeCoupon(@PathVariable("code") String code){
        userCouponService.exchangeCoupon(code);
    }

    //给方法是给ti-trade服务 远程调用使用的
    @ApiOperation("查询我的可用优惠券方案")
    @PostMapping("avaliable")
    public List<CouponDiscountDTO> findDiscountSolution(@RequestBody List<OrderCourseDTO> orderCourses){
        return userCouponService.findDiscountSolution(orderCourses);
    }

}
