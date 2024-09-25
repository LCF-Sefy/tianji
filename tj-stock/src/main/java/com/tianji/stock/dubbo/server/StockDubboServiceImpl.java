package com.tianji.stock.dubbo.server;

import com.tianji.api.interfaces.stock.StockDubboService;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.stock.domain.dto.CouponStockDTO;
import com.tianji.stock.service.IStockService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

@DubboService(version = "1.0.0", interfaceClass = StockDubboService.class)
@Slf4j
public class StockDubboServiceImpl implements StockDubboService {

    @Autowired
    private IStockService stockService;

    /**
     * 设置商品库存
     *
     * @param couponId
     * @param stock
     */
    @Override
    public void setCouponStock(Long couponId, Integer stock, Long userId) {
        log.info("设置商品库存，couponId:{},stock:{}", couponId, stock);
        if (couponId == null || stock == null || userId == null) {
            throw new BadRequestException("参数不能为空");
        }
        CouponStockDTO dto = new CouponStockDTO();
        dto.setCouponId(couponId);
        dto.setTotalNum(stock);
        dto.setCreater(userId);
        dto.setUpdater(userId);
        stockService.setCouponStock(dto);
        log.info("设置商品库存成功");
    }

    /**
     * 更新已领取数量+1（扣减库存）
     *
     * @param creater  （商家id，商家优惠券是按照商家进行分库分表，库存表按照优惠券id进行分库分表）
     * @param couponId 优惠券id
     * @return
     */
    @Override
    public int increaseInssueNum(Long creater, Long couponId) {
        log.info("更新已领取数量+1，creater:{},couponId:{}", creater, couponId);
        return stockService.increaseInssueNum(creater, couponId);
    }

}
