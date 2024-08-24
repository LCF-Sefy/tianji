package com.tianji.api.client.promotion;

import com.tianji.api.client.promotion.fallback.PromotionClientFallback;
import com.tianji.api.dto.promotion.CouponDiscountDTO;
import com.tianji.api.dto.promotion.OrderCourseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

//促销服务feign客户端
@FeignClient(value = "promotion-service",fallbackFactory = PromotionClientFallback.class)
public interface PromotionClient {

    /**
     * 查询我的可用优惠券方案
     * @param orderCourses
     * @return
     */
    @PostMapping("/user-coupons/avaliable")
    public List<CouponDiscountDTO> findDiscountSolution(@RequestBody List<OrderCourseDTO> orderCourses);
}
