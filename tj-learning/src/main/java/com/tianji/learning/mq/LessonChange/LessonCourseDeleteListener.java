package com.tianji.learning.mq.LessonChange;

import com.tianji.api.dto.trade.OrderBasicDTO;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.utils.CollUtils;
import com.tianji.learning.service.ILearningLessonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor  //使用构造器 Lombok是在编译期生成相应的方法
@RocketMQMessageListener(
        topic = MqConstants.Topic.COURSE_TOPIC,
        consumerGroup = MqConstants.ConsumerGroup.USER_COURSE_DELETE,
        selectorExpression = MqConstants.Tag.USER_COURSE_DELETE,
        messageModel = MessageModel.CLUSTERING)
public class LessonCourseDeleteListener implements RocketMQListener<OrderBasicDTO> {

    final ILearningLessonService lessonService;

//    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "learning.lesson.refund.queue", durable = "true"),
//            exchange = @Exchange(value = MqConstants.Exchange.ORDER_EXCHANGE, type = ExchangeTypes.TOPIC),
//            key = MqConstants.Key.ORDER_REFUND_KEY))
//    public void onDeleteCourseMsg(OrderBasicDTO dto) {
//        log.info("LessonChangeListener 接收到了消息 用户{}的{}订单，需要删除课程{}", dto.getUserId(), dto.getOrderId(), dto.getCourseIds());
//        //1.校验参数
//        if (dto == null ||
//                dto.getUserId() == null ||
//                dto.getOrderId() == null ||
//                CollUtils.isEmpty(dto.getCourseIds())) {
//            // 不要抛异常，否则会本地重试
//            log.error("接收到MQ消息有误，订单数据为空");
//            return;
//        }
//
//        //2.调用service 删除用户课程表中的课程
//        lessonService.deleteUserLesson(dto.getUserId(), dto.getCourseIds().get(0));
//    }

    //消费者端监听mq中的消息，将课程从用户课表中删除
    @Override
    public void onMessage(OrderBasicDTO msg) {
        log.info("LessonChangeListener 接收到了消息 用户{}的{}订单，需要删除课程{}", msg.getUserId(), msg.getOrderId(), msg.getCourseIds());
        //1.校验参数
        if (msg == null ||
                msg.getUserId() == null ||
                msg.getOrderId() == null ||
                CollUtils.isEmpty(msg.getCourseIds())) {
            // 不要抛异常，否则会本地重试
            log.error("接收到MQ消息有误，订单数据为空");
            return;
        }

        //2.调用service 删除用户课程表中的课程
        lessonService.deleteUserLesson(msg.getUserId(), msg.getCourseIds().get(0));
    }
}
