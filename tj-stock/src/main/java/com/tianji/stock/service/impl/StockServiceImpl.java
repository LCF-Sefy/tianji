package com.tianji.stock.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.stock.domain.dto.CouponStockDTO;
import com.tianji.stock.domain.po.CouponStock;
import com.tianji.stock.mapper.StockMapper;
import com.tianji.stock.service.IStockService;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
public class StockServiceImpl extends ServiceImpl<StockMapper, CouponStock> implements IStockService {

    @Autowired
    private StockMapper stockMapper;


    /**
     * 商家设置优惠券库存
     *
     * @param dto
     */
    @Override
    public void setCouponStock(CouponStockDTO dto) {
        if (dto == null) {
            throw new BadRequestException("参数为空");
        }
        CouponStock stock = BeanUtils.copyBean(dto, CouponStock.class);
        this.save(stock);
    }

    /**
     * 更新已领取数量+1（扣减库存）
     *
     * @param creater  （商家id，商家优惠券是按照商家进行分库分表，库存表按照优惠券id进行分库分表）
     * @param couponId 优惠券id
     */
    @Override
    public int increaseInssueNum(Long creater, Long couponId) {
        return stockMapper.increaseInssueNum(creater, couponId);
    }


}
