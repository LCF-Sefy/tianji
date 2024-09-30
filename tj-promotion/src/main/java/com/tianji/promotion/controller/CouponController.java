package com.tianji.promotion.controller;


import com.jd.platform.hotkey.client.callback.JdHotKeyStore;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.common.ratelimiter.annotation.TjRateLimiter;
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
    @TjRateLimiter(permitsPerSecond = 1000, timeout = 0)
    public CouponVO detail(@PathVariable("shopId") Long shopId, @PathVariable("couponId") Long couponId){
        return couponService.detail(shopId, couponId);
    }


    //测试hotkey的接入
    @PostMapping("hotkey")
    public Object hotkey(@RequestParam("key") String key) {
        //是hotkey则从本地缓存中取
        if (JdHotKeyStore.isHotKey(key)){
            Object value = JdHotKeyStore.get(key);
            //如果value为null，则需要从缓存中拿数据然后设置到本地缓存，应当使用分布式所。
            if (value == null) {
                //获取分布式锁

                //获取失败，则稍后再试

                //获取成功，则从缓存中拿数据，然后设置到本地缓存
                value = "从缓存中取出更新到本地缓存";
                JdHotKeyStore.smartSet(key, value);
                return value;
            }
            return "从本地缓存中取到：" + value;
        }
        //不是hotkey则从缓存中取
        return "不是hotkey，从缓存中取";
    }
}
