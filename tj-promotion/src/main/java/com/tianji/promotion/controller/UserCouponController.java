package com.tianji.promotion.controller;


import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.tianji.api.interfaces.learning.LearningDubboService;
import com.tianji.promotion.domain.dto.CouponDiscountDTO;
import com.tianji.promotion.domain.dto.CouponFormDTO;
import com.tianji.promotion.domain.dto.OrderCourseDTO;
import com.tianji.promotion.enums.DiscountType;
import com.tianji.promotion.enums.ObtainType;
import com.tianji.promotion.service.ICouponService;
import com.tianji.promotion.service.IUserCouponService;
import io.seata.spring.annotation.GlobalTransactional;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
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
    private final ICouponService couponService;

    @DubboReference(version = "1.0.0")
    private LearningDubboService learningDubboService;

    @ApiOperation("领取优惠劵")
    @PostMapping("{id}/receive")
    public void receiveCoupon(@PathVariable Long id) {
        userCouponService.receiveCoupon(id);
    }

    /**
     * 根据shopId 和 id领取优惠券
     * @param shopId  商家id（creater）
     * @param id      优惠券id
     */
    @ApiOperation("领取优惠劵升级版")
    @PostMapping("receive/{shopId}/{id}")
    public void receiveCoupon(@PathVariable("shopId") Long shopId, @PathVariable("id") Long id ) {
        userCouponService.receiveCouponByShopIdAndId(shopId, id);
    }

    @ApiOperation("兑换码兑换优惠券")
    @PostMapping("/{code}/exchange")
    public void exchangeCoupon(@PathVariable("code") String code) {
        userCouponService.exchangeCoupon(code);
    }

    //给方法是给ti-trade服务 远程调用使用的
    @ApiOperation("查询我的可用优惠券方案")
    @PostMapping("avaliable")
    public List<CouponDiscountDTO> findDiscountSolution(@RequestBody List<OrderCourseDTO> orderCourses) {
        return userCouponService.findDiscountSolution(orderCourses);
    }


    //以下均为测试

    /**
     * 测试dubbo
     */
    @GetMapping("/hello")
    public void sayhello() {
        learningDubboService.sayHello("超凡");
    }

    /**
     * 测试senta分布式事务
     */
    @PostMapping("/seate")
    @GlobalTransactional(rollbackFor = Exception.class)
    public void seate() {
        CouponFormDTO dto = new CouponFormDTO();
        dto.setId(1799677743084539999L);
        dto.setName("测试优惠券");
        dto.setDiscountType(DiscountType.PRICE_DISCOUNT);
        dto.setSpecific(false);
        dto.setDiscountValue(1000);
        dto.setThresholdAmount(100000);
        dto.setMaxDiscountAmount(0);
        dto.setObtainWay(ObtainType.PUBLIC);
        couponService.saveCoupon(dto);
        learningDubboService.testSeate();
    }

    /**
     * 测试sentinel限流
     *
     * @return
     */
    @GetMapping("/limit")
    @SentinelResource(value = "QUEUE-DATA-FLOW")
    public String limit() {
        return "hello";
    }
}
