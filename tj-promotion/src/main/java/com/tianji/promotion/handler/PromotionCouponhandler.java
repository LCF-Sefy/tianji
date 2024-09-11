package com.tianji.promotion.handler;

import com.tianji.common.constants.MqConstants;
import com.tianji.promotion.domain.dto.UserCouponDTO;
import com.tianji.promotion.domain.po.UserCoupon;
import com.tianji.promotion.service.IUserCouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.annotation.SelectorType;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
@Slf4j
@RequiredArgsConstructor
@RocketMQMessageListener(topic = MqConstants.Topic.PROMOTION_TOPIC,
        consumerGroup = "${rocketmq.consumer.group}",
        messageModel = MessageModel.CLUSTERING)
public class PromotionCouponhandler implements RocketMQListener<UserCouponDTO> {

    private final IUserCouponService userCouponService;

//    @RabbitListener(bindings = @QueueBinding(
//            value = @Queue(value = "coupon.receive.queue", durable = "true"),
//            exchange = @Exchange(value = MqConstants.Exchange.PROMOTION_EXCHANGE, type = ExchangeTypes.TOPIC),
//            key = MqConstants.Key.COUPON_RECEIVE))
//    public void OnMsg(UserCouponDTO msg) {
//        log.debug("收到领券的消息：{}", msg);
//        userCouponService.checkAndCreateUserCouponNew(msg);
//    }

    @Override
    public void onMessage(UserCouponDTO msg) {
        log.debug("收到领券的消息：{}", msg);
        userCouponService.checkAndCreateUserCouponNew(msg);
    }
}
