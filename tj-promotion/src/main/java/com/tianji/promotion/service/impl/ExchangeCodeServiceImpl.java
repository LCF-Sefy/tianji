package com.tianji.promotion.service.impl;

import com.tianji.common.utils.CollUtils;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.tianji.promotion.mapper.ExchangeCodeMapper;
import com.tianji.promotion.service.IExchangeCodeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.promotion.utils.CodeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.tianji.promotion.constants.PromotionConstants.*;

/**
 * <p>
 * 兑换码 服务实现类
 * </p>
 *
 * @author sefy
 * @since 2024-05-15
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ExchangeCodeServiceImpl extends ServiceImpl<ExchangeCodeMapper, ExchangeCode> implements IExchangeCodeService {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 异步生成兑换码
     *
     * @param coupon
     */
    @Override
    @Async("generateExchangeCodeExecutor")  //使用自定义线程池中的线程异步执行
    public void asycGenerateExchangeCode(Coupon coupon) {
        log.debug("生成兑换码 线程名：{}", Thread.currentThread().getName());   //自定义线程池中的线程
        //0.获取需要生成的兑换码的总数量 - 即优惠券的发放总数量
        Integer totalNum = coupon.getTotalNum();

        //1.生成自增id  借助redis 的 incr自增需要生成的兑换码的总数量
        Long increment = stringRedisTemplate.opsForValue().increment(COUPON_CODE_SERIAL_KEY, totalNum);
        if (increment == null) {
            return;
        }
        int maxSerialNum = increment.intValue(); //本次自增id 的最大值
        int begin = maxSerialNum - totalNum + 1;

        //2.循环生成兑换码 调用工具类生成兑换码
        List<ExchangeCode> list = new ArrayList<>();
        for (int serialNum = begin; serialNum <= maxSerialNum; serialNum++) {
            String code = CodeUtil.generateCode(serialNum, coupon.getId());//参数1为自增id值，参数二为优惠券id（内部会计算出0-15之间的数字）
            ExchangeCode exchangeCode = new ExchangeCode();
            exchangeCode.setId(serialNum); //兑换码id - 自增长的id：serialNum
            exchangeCode.setExchangeTargetId(coupon.getId());   //对应的优惠券id
            exchangeCode.setCode(code);   //生成的兑换码
            exchangeCode.setExpiredTime(coupon.getIssueEndTime());//兑换码兑换的截止时间，就是优惠券领取的截至时间
            list.add(exchangeCode);
        }

        //3.将兑换码保存到db 批量保存到exchange_code表
        this.saveBatch(list);

        // 4.写入Redis缓存，member：couponId，score：兑换码的最大序列号 （本次生成兑换码可省略）
        stringRedisTemplate.opsForZSet().add(COUPON_RANGE_KEY, coupon.getId().toString(), maxSerialNum);
    }

    /**
     * 修改兑换码状态：是否领取的状态 操作redis的bitmap
     *
     * @param couponId
     * @param flag
     * @return
     */
    @Override
    public boolean updateExchangeCodeMark(long couponId, boolean flag) {
        //setbit key offset 1 返回true代表已经兑换
        //aBoolean返回的是更新前的状态 如果更新前是未兑换0 则返回false  已兑换1则返回true
        Boolean aBoolean = stringRedisTemplate.opsForValue().setBit(COUPON_CODE_MAP_KEY, couponId, flag);
        return aBoolean != null && aBoolean;
    }

    @Override
    public Long exchangeTargetId(long serialNum) {
        // 1.查询score值比当前序列号大的第一个优惠券
        Set<String> results = stringRedisTemplate.opsForZSet().rangeByScore(
                COUPON_RANGE_KEY, serialNum, serialNum + 5000, 0L, 1L);
        if (CollUtils.isEmpty(results)) {
            return null;
        }
        // 2.数据转换
        String next = results.iterator().next();
        return Long.parseLong(next);
    }
}
