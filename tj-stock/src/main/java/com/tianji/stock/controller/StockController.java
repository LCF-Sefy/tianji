package com.tianji.stock.controller;

import com.tianji.stock.domain.dto.CouponStockDTO;
import com.tianji.stock.service.IStockService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "库存相关接口")
@RestController
@RequestMapping("/stock")
@RequiredArgsConstructor
public class StockController {

    private final IStockService stockService;

    @ApiOperation("设置优惠券的库存-管理端")
    @PostMapping("/set")
    public void setCouponStock(@RequestBody @Validated CouponStockDTO dto) {
        stockService.setCouponStock(dto);
    }
}
