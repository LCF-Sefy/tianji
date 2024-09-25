package com.tianji.learning.mq.LikedRecord;

import com.tianji.api.dto.msg.LikedTimesDTO;
import com.tianji.api.dto.msg.LikedTimesMessage;
import com.tianji.common.constants.MqConstants;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.service.IInteractionReplyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.tianji.common.constants.MqConstants.ConsumerGroup.LIKED_RECORD_GROUP;

/**
 * 消费消息 监听QA点赞消息然后消费
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = MqConstants.Topic.LIKE_RECORD_TOPIC, consumerGroup = LIKED_RECORD_GROUP,
        messageModel = MessageModel.CLUSTERING)
public class LikedRecordListener implements RocketMQListener<LikedTimesMessage> {

    private final IInteractionReplyService replyService;

    /**
     * QA问答系统 消费者   监听业务点赞数量消息
     */
//    @RabbitListener(bindings = @QueueBinding(
//            value = @Queue(value = "qa.liked.times.queue", durable = "true"),
//            exchange = @Exchange(value = MqConstants.Exchange.LIKE_RECORD_EXCHANGE, type = ExchangeTypes.TOPIC),
//            key = MqConstants.Key.QA_LIKED_TIMES_KEY))
//    public void onMsg(List<LikedTimesDTO> list) {
//        log.debug("LikedRecordListener 监听到消息，{}", list);
//        //消息转po
//        List<InteractionReply> replyList = new ArrayList<>();
//        for (LikedTimesDTO dto : list) {
//            InteractionReply reply = new InteractionReply();
//            reply.setId(dto.getBizId());
//            reply.setLikedTimes(dto.getLikedTimes());
//            replyList.add(reply);
//        }
//        replyService.updateBatchById(replyList);
//    }
    @Override
    public void onMessage(LikedTimesMessage msg) {
        log.debug("LikedRecordListener 监听到消息，{}", msg);
//        LikedTimesMessage msg = JsonUtils.toBean(msgStr, LikedTimesMessage.class);
        List<LikedTimesDTO> list = msg.getLikedTimesDTOList();
        String bizType = msg.getBizType();
        //消息转po
        if (bizType.equals("QA")) {
            List<InteractionReply> replyList = new ArrayList<>();
            for (LikedTimesDTO dto : list) {
                InteractionReply reply = new InteractionReply();
                reply.setId(dto.getBizId());
                reply.setLikedTimes(dto.getLikedTimes());
                replyList.add(reply);
            }
            replyService.updateBatchById(replyList);
        }
    }

    /*public void onMsg(LikedTimesDTO dto) {
        log.debug("LikedRecordListener 监听到消息，{}", dto);
        InteractionReply reply = replyService.getById(dto.getBizId());
        if (reply == null) {
            return;
        }
        reply.setLikedTimes(dto.getLikedTimes());
        replyService.updateById(reply);
    }*/
}
