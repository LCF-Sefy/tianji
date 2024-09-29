package com.tianji.common.constants;

public interface MqConstants {
    interface Exchange {
        /*课程有关的交换机*/
        String COURSE_EXCHANGE = "course.topic";

        /*订单有关的交换机*/
        String ORDER_EXCHANGE = "order.topic";

        /*学习有关的交换机*/
        String LEARNING_EXCHANGE = "learning.topic";

        /*信息中心短信相关的交换机*/
        String SMS_EXCHANGE = "sms.direct";

        /*异常信息的交换机*/
        String ERROR_EXCHANGE = "error.topic";

        /*支付有关的交换机*/
        String PAY_EXCHANGE = "pay.topic";
        /*交易服务延迟任务交换机*/
        String TRADE_DELAY_EXCHANGE = "trade.delay.topic";

        /*点赞记录有关的交换机*/
        String LIKE_RECORD_EXCHANGE = "like.record.topic";

        /*促销服务有关的交换机*/
        String PROMOTION_EXCHANGE = "promotion.topic";
    }

    interface Queue {
        String ERROR_QUEUE_TEMPLATE = "error.{}.queue";
    }

    interface Key {
        /*课程有关的 RoutingKey*/
        String COURSE_NEW_KEY = "course.new";
        String COURSE_UP_KEY = "course.up";
        String COURSE_DOWN_KEY = "course.down";
        String COURSE_EXPIRE_KEY = "course.expire";
        String COURSE_DELETE_KEY = "course.delete";

        /*订单有关的RoutingKey*/
        String ORDER_PAY_KEY = "order.pay";
        String ORDER_REFUND_KEY = "order.refund";

        /*积分相关RoutingKey*/
        /* 写回答 */
        String WRITE_REPLY = "reply.new";
        /* 签到 */
        String SIGN_IN = "sign.in";
        /* 学习视频 */
        String LEARN_SECTION = "section.learned";
        /* 写笔记 */
        String WRITE_NOTE = "note.new";
        /* 笔记被采集 */
        String NOTE_GATHERED = "note.gathered";

        /*点赞的RoutingKey*/
        String LIKED_TIMES_KEY_TEMPLATE = "{}.times.changed";
        /*问答*/
        String QA_LIKED_TIMES_KEY = "QA.times.changed";
        /*笔记*/
        String NOTE_LIKED_TIMES_KEY = "NOTE.times.changed";

        /*短信系统发送短信*/
        String SMS_MESSAGE = "sms.message";

        /*异常RoutingKey的前缀*/
        String ERROR_KEY_PREFIX = "error.";
        String DEFAULT_ERROR_KEY = "error.#";

        /*支付有关的key*/
        String PAY_SUCCESS = "pay.success";
        String REFUND_CHANGE = "refund.status.change";

        String ORDER_DELAY_KEY = "delay.order.query";

        /*领取优惠券的key*/
        String COUPON_RECEIVE = "coupon.receive";

    }


    //rocketmq相关
    interface Topic {
        /*学习有关topic*/
        String LEARN_TOPIC = "learn";

        /*QA:点赞记录有关topic*/
        String LIKE_RECORD_TOPIC = "like_record";

        /*订单有关topic*/
        String ORDER_TOPIC = "order";

        /*课程有关topic*/
        String COURSE_TOPIC = "course";

        /*促销服务有关topic*/
        String PROMOTION_TOPIC = "promotion";
    }

    interface Tag{

        /**
         * 用户课程有关tag
         */
        String USER_COURSE_DELETE = "delete";
        String USER_COURSE_SAVE = "save";

        /*订单有关*/
        String ORDER_PAY_TAG = "order.pay";
        String ORDER_REFUND_TAG = "order.refund";

    }

    interface ConsumerGroup{
        String SIGN_GROUP = "consumer_group_sign";
        String LIKED_RECORD_GROUP = "consumer_group_liked_record";

        /*用户课程有关消费组*/
        String USER_COURSE_DELETE = "consumer_group_delete_course";
        String USER_COURSE_SAVE = "consumer_group_save_course";

        /**
         * 用户优惠券相关消费组
         */
        String USER_COUPON_SAVE = "consumer_group_save_user_coupon";
        String USER_COUPON_DELETE = "consumer_group_delete_user_coupon";

        /**
         * 库存有关消费组
         */
        String COUPON_STOCK_REDUCE = "consumer_group_reduce_coupon_stock";
        String COUPON_STOCK_ADD = "consumer_group_add_coupon_stock";
    }
}
