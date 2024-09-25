package com.tianji.stock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianji.stock.domain.dto.CouponStockDTO;
import com.tianji.stock.domain.po.CouponStock;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StockMapper extends BaseMapper<CouponStock> {

    int increaseInssueNum(@Param("creater") Long creater, @Param("couponId") Long couponId);
}
