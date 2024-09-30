package com.tianji.promotion.handler;

import com.tianji.common.constants.MqConstants;
import com.tianji.common.domain.mq.CouponMessage;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.promotion.domain.dto.UserCouponDTO;
import com.tianji.promotion.service.IUserCouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
@RocketMQMessageListener(topic = MqConstants.Topic.PROMOTION_TOPIC,
        consumerGroup = MqConstants.ConsumerGroup.USER_COUPON_SAVE,
        messageModel = MessageModel.CLUSTERING)
public class PromotionCouponListener implements RocketMQListener<CouponMessage> {

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
    public void onMessage(CouponMessage msg) {
        log.debug("接收保存用户优惠券的消息：{}", msg);
        UserCouponDTO dto = BeanUtils.copyBean(msg, UserCouponDTO.class);
        userCouponService.checkAndCreateUserCouponNew(dto);
    }
}
