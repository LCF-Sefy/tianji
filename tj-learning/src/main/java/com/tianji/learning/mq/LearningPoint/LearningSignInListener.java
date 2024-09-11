package com.tianji.learning.mq.LearningPoint;

import com.tianji.common.constants.MqConstants;
import com.tianji.learning.domain.msg.PointMessage;
import com.tianji.learning.domain.msg.SignInMessage;
import com.tianji.learning.enums.PointsRecordType;
import com.tianji.learning.service.IPointsRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import static com.tianji.common.constants.MqConstants.ConsumerGroup.SIGN_GROUP;

/**
 * 消费消息 消费增加积分的消息
 */
@Component
@Slf4j
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = MqConstants.Topic.LEARN_TOPIC, consumerGroup = SIGN_GROUP,
        messageModel = MessageModel.CLUSTERING)
public class LearningSignInListener implements RocketMQListener<PointMessage> {

    private final IPointsRecordService pointsRecordService;

    /**
     * 监听签到增加的积分mq消息
     *
     * @param msg
     */
    /*@RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "sign.points.queue", durable = "true"),
            exchange =@Exchange(value = MqConstants.Exchange.LEARNING_EXCHANGE,type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.SIGN_IN))
    public void listenSignInListener(SignInMessage msg){
        log.debug("签到增加到积分，监听到消息：{}",msg);
        pointsRecordService.addPointRecord(msg, PointsRecordType.SIGN);
    }*/
    @Override
    public void onMessage(PointMessage msg) {
        log.info("监听到增加积分的消息：{}", msg);
        if (msg == null) {
            log.error("监听到增加积分的消息为空");
        }
        pointsRecordService.addPointRecord(SignInMessage.of(msg.getUserId(), msg.getPoints()), PointsRecordType.of(msg.getType()));
    }
}