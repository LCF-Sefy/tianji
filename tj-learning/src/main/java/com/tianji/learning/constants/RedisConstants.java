package com.tianji.learning.constants;

public interface RedisConstants {

    /**
     * 签到记录的key的前缀  完整的key为：sign:uid:用户id：年月
     */
    String SIGN_RECORD_KEY_PREFIX = "sign:uid:";

    /**
     * 积分排行榜key前缀 完整格式为  boards：年月
     */
    String POINTS_BOARD_KEY_PREFIX = "boards:";
}
