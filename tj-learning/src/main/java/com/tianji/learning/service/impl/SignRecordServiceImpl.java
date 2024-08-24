package com.tianji.learning.service.impl;

import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.domain.vo.SignResultVO;
import com.tianji.learning.mq.msg.SignInMessage;
import com.tianji.learning.service.ISignRecordService;
import io.swagger.models.auth.In;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SignRecordServiceImpl implements ISignRecordService {

    private final StringRedisTemplate redisTemplate;
    private final RabbitMqHelper mqHelper;

    @Override
    public SignResultVO addSignRecords() {
        //1.获取当前登录用户
        Long userId = UserContext.getUser();

        //2.拼接key
        LocalDateTime now = LocalDateTime.now();  //当前时间的年月
        String format = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));   //得到 冒号年月 格式字符串
        String key = RedisConstants.SIGN_RECORD_KEY_PREFIX + userId.toString() + format;

        //3.利用bitset命令 将签到记录保存到redis的bitmap结构中   需要判断返回值，如果返回true说明已经签到
        int offset = now.getDayOfMonth() - 1;
        Boolean setBit = redisTemplate.opsForValue().setBit(key, offset, true);
        if (setBit) {
            //setBit为true说明已经签过到了
            throw new BizIllegalException("不能重复签到");
        }

        //4.计算连续签到的天数
        int days = countContinuousSignDays(key, now.getDayOfMonth());

        //5.计算连续签到的奖励积分
        int rewardPoints = 0;
        switch (days) {
            case 7:
                rewardPoints = 10;
                break;
            case 14:
                rewardPoints = 20;
                break;
            case 28:
                rewardPoints = 40;
                break;
        }

        //6.保存积分 发送消息到mq
        mqHelper.send(MqConstants.Exchange.LEARNING_EXCHANGE,
                MqConstants.Key.SIGN_IN,
                SignInMessage.of(userId, rewardPoints + 1));

        //7.封装vo返回
        SignResultVO vo = new SignResultVO();
        vo.setSignDays(days);
        vo.setRewardPoints(rewardPoints);
        return vo;
    }

    /**
     * 计算当前登录用户连续签到天数
     *
     * @param key        缓存中的key
     * @param dayOfMonth 本月第一天到今天的天数
     * @return
     */
    private int countContinuousSignDays(String key, int dayOfMonth) {
        //1.求本月第一天到今天所有的签到数据 bitfield  得到的是十进制
        //bitfield key get u天数 0
        List<Long> bitField = redisTemplate.opsForValue().bitField(key,
                BitFieldSubCommands.create().get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0));
        if (CollUtils.isEmpty(bitField)) {
            return 0;
        }
        Long num = bitField.get(0); //本月第一天到今天的签到数据  十进制
        log.debug("num:{}", num);
        //2.num转二进制 从后往前推共有多少个连续1
        int counter = 0; //计数器
        while ((num & 1) == 1) {
            counter++;
            num >>>= 1;  //num右移一位
        }
        return counter;
    }

    /**
     * 查询当前登录用户本月的签到记录
     *
     * @return
     */
    @Override
    public Byte[] querySignRecords() {
        //1.获取当前登录用户
        Long userId = UserContext.getUser();

        //2.拼接key
        LocalDateTime now = LocalDateTime.now();  //当前时间的年月
        String format = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));   //得到 冒号年月 格式字符串
        String key = RedisConstants.SIGN_RECORD_KEY_PREFIX + userId.toString() + format;

        //3.从Redis中查询本月的签到记录
        int dayOfMonth = now.getDayOfMonth();
        List<Long> bitField = redisTemplate.opsForValue().bitField(key,
                BitFieldSubCommands.create().get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0));
        if (CollUtils.isEmpty(bitField)) {
            return new Byte[0];
        }
        Long data = bitField.get(0); //拿到本月这三十天的签到数据  十进制
        int offset = dayOfMonth - 1;

        //4.封装为List返回
        Byte[] arr = new Byte[dayOfMonth];
        while (offset >= 0) {
            arr[offset] = (byte) (data & 1);
            offset--;
            data = data >>> 1;   //右移一位
        }
        return arr;
    }
}
