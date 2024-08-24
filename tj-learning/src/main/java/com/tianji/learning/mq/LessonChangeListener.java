package com.tianji.learning.mq;

import com.tianji.api.dto.trade.OrderBasicDTO;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.utils.CollUtils;
import com.tianji.learning.service.ILearningLessonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Slf4j
@RequiredArgsConstructor  //使用构造器 Lombok是在编译期生成相应的方法
public class LessonChangeListener {

    final ILearningLessonService lessonService;

    //消费者端监听mq中的消息，将课程加入到用户课表
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "learning.lesson.pay.queue", durable = "true"),
            exchange = @Exchange(value = MqConstants.Exchange.ORDER_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.ORDER_PAY_KEY))
    public void onMsg(OrderBasicDTO dto) {
        log.info("LessonChangeListener 接收到了消息 用户{}，添加课程{}", dto.getUserId(), dto.getCourseIds());
        //1.校验参数
        if (dto == null ||
                dto.getUserId() == null ||
                dto.getOrderId() == null ||
                CollUtils.isEmpty(dto.getCourseIds())) {
            // 不要抛异常 否则开启重试
            log.error("接收到MQ消息有误，订单数据为空");
            return;
        }

        //2.调用service 保存课程到课表
        lessonService.addUserLesson(dto.getUserId(), dto.getCourseIds());
    }

    //消费者端监听mq中的消息，将课程从用户课表中删除
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "learning.lesson.refund.queue", durable = "true"),
            exchange = @Exchange(value = MqConstants.Exchange.ORDER_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.ORDER_REFUND_KEY))
    public void onDeleteCourseMsg(OrderBasicDTO dto) {
        log.info("LessonChangeListener 接收到了消息 用户{}的{}订单，需要删除课程{}", dto.getUserId(), dto.getOrderId(), dto.getCourseIds());
        //1.校验参数
        if (dto == null ||
                dto.getUserId() == null ||
                dto.getOrderId() == null ||
                CollUtils.isEmpty(dto.getCourseIds())) {
            // 不要抛异常，否则会本地重试
            log.error("接收到MQ消息有误，订单数据为空");
            return;
        }

        //2.调用service 删除用户课程表中的课程
        lessonService.deleteUserLesson(dto.getUserId(), dto.getCourseIds().get(0));
    }
}
