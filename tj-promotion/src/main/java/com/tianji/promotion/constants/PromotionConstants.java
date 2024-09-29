package com.tianji.promotion.constants;

public interface PromotionConstants {
    String COUPON_CODE_SERIAL_KEY = "coupon:code:serial";  //自增长id 对应的键
    String COUPON_CODE_MAP_KEY = "coupon:code:map"; //校验兑换码是否兑换 bitmap
    String COUPON_CACHE_KEY_PREFIX = "prs:coupon:";
    String COUPON_RANGE_KEY = "coupon:code:range";


    //配置库存和扣减库存会用          {%s}保证在集群计算key时都根据优惠券id计算，能路由到同一个分片
    String SHOP_COUPON_CHECK_KEY_FORMAT = "prs:check:coupon:%s:{%s}";
    String SHOP_COUPON_DETAIL_KEY_FORMAT = "prs:detail:coupon:%s:{%s}";

    //prs:issued:coupon:商家id:优惠券id      如果要对大key拆分在后面加上:hash(field)%大key的拆分数量          这里的field为用户id
    String SHOP_COUPON_ISSUED_KEY_FORMAT = "prs:issued:coupon:%s:{%s}";

    //根据商家id计算key
    String SHOP_COUPON_LIST_KEY_FORMAT = "prs:list:coupon:{%s}:%s"; //商家id:优惠券状态


    String COUPON_LOCAL_CACHE_KEY_FORMAT = "%s:%s";  //商家id：优惠券id
    String SHOP_COUPON_LIST_LOCAL_CACHE_KEY_FORMAT = "%s:%s";     //商家id:优惠券状态

    /**
     * 优惠券详情分布式缓存更新时的分布式锁key模板
     */
    String COUPON_CACHE_UPDATE_LOCK_KEY_FORMAT = "COUPON_CACHE_UPDATE_LOCK_KEY:%s:%s";

    /**
     * 商家优惠券列表分布式缓存更新时的分布式锁key模板
     */
    String SHOP_COUPON_LIST_CACHE_UPDATE_LOCK_KEY_FORMAT = "SHOP_COUPON_LIST_CACHE_UPDATE_LOCK_KEY:%s:%s";

    Long FIVE_MINUTES = 5 * 60L;

    String[] RECEIVE_COUPON_ERROR_MSG = {
            "活动未开始",
            "库存不足",
            "活动已经结束",
            "领取次数过多",
    };
    String[] EXCHANGE_COUPON_ERROR_MSG = {
            "兑换码已兑换",
            "无效兑换码",
            "活动未开始",
            "活动已经结束",
            "领取次数过多",
    };
}