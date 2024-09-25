package com.tianji.promotion.constants;

public interface PromotionConstants {
    String COUPON_CODE_SERIAL_KEY = "coupon:code:serial";  //自增长id 对应的键
    String COUPON_CODE_MAP_KEY = "coupon:code:map"; //校验兑换码是否兑换 bitmap
    String COUPON_CACHE_KEY_PREFIX = "prs:coupon:";
    String COUPON_RANGE_KEY = "coupon:code:range";

    String SHOP_COUPON_CACHE_KEY_PREFIX = "prs:shop:coupon:";

    String SHOP_COUPON_CHECK_KEY_FORMAT = "prs:check:coupon:%s:%s";
    String SHOP_COUPON_DETAIL_KEY_FORMAT = "prs:detail:coupon:%s:%s";
    String SHOP_COUPON_ISSUED_KEY_FORMAT = "prs:issued:coupon:%s:%s";

    String SHOP_COUPON_LIST_KEY_FORMAT = "prs:list:coupon:%s:%s"; //商家id:优惠券状态


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