package com.itheima;

import com.tianji.common.cache.distribute.DistributedCacheService;
import com.tianji.learning.LearningApplication;
import com.tianji.learning.constants.RedisConstants;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest(classes = LearningApplication.class)
public class RedisBitMapTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private DistributedCacheService distributedCacheService;

    @Test
    public void test() {
        //1. 获取上月时间
        LocalDateTime time = LocalDateTime.now().minusMonths(1);

        //2.计算key
        String format = time.format(DateTimeFormatter.ofPattern("yyyyMM"));
        String key = RedisConstants.POINTS_BOARD_KEY_PREFIX + format; //key= boards：上赛季的年月
        for (int i = 1; i <= 20; i++) {
            redisTemplate.opsForZSet().add(key, String.valueOf(i), i);
        }

    }

    @Test
    public void test2(){
        Map<String, String> keyValues = new HashMap<>();
        keyValues.put("key1{1}", "value1");
        keyValues.put("key2{1}", "value2");
        keyValues.put("key3{1}", "value3");
        redisTemplate.opsForValue().multiSet(keyValues);
    }
}
