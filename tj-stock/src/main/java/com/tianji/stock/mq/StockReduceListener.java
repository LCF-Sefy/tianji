package com.tianji.stock.mq;

import com.tianji.common.constants.MqConstants;
import com.tianji.common.domain.mq.CouponMessage;
import com.tianji.common.exceptions.DbException;
import com.tianji.stock.service.IStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@RocketMQMessageListener(topic = MqConstants.Topic.PROMOTION_TOPIC,
        consumerGroup = MqConstants.ConsumerGroup.COUPON_STOCK_REDUCE,
        messageModel = MessageModel.CLUSTERING)
public class StockReduceListener implements RocketMQListener<CouponMessage> {

    @Autowired
    private IStockService stockService;

    @Override
    public void onMessage(CouponMessage message) {
        log.info("接收到优惠活动库存扣减消息: {}", message);
        int rows = stockService.increaseInssueNum(message.getCreater(), message.getCouponId());
        if (rows == 0) {
            throw new DbException("库存扣减失败");
        }
    }
}
