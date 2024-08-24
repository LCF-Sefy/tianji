package com.tianji.learning.mq;

import com.tianji.common.constants.MqConstants;
import com.tianji.learning.enums.PointsRecordType;
import com.tianji.learning.mq.msg.SignInMessage;
import com.tianji.learning.service.IPointsBoardService;
import com.tianji.learning.service.IPointsRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 消费消息 监听mq 用于保存积分
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LearningPointListener {

    private final IPointsRecordService pointsRecordService;

    /**
     * 监听签到增加的积分mq消息
     * @param msg
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "sign.points.queue", durable = "true"),
            exchange =@Exchange(value = MqConstants.Exchange.LEARNING_EXCHANGE,type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.SIGN_IN))
    public void listenSignInListener(SignInMessage msg){
        log.debug("签到增加到积分，监听到消息：{}",msg);
        pointsRecordService.addPointRecord(msg, PointsRecordType.SIGN);
    }

    /**
     * 监听问答增加的积分mq消息
     * @param msg
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "qa.points.queue", durable = "true"),
            exchange =@Exchange(value = MqConstants.Exchange.LEARNING_EXCHANGE,type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.WRITE_REPLY))
    public void listenReplyListener(SignInMessage msg){
        log.debug("问答增加到积分，监听到消息：{}",msg);
        pointsRecordService.addPointRecord(msg, PointsRecordType.QA);

    }
}
