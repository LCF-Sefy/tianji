//package com.tianji.promotion.service.impl;
//
//import cn.hutool.core.bean.copier.CopyOptions;
//import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
//import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
//import com.tianji.common.constants.MqConstants;
//import com.tianji.common.exceptions.BadRequestException;
//import com.tianji.common.exceptions.BizIllegalException;
//import com.tianji.common.utils.BeanUtils;
//import com.tianji.common.utils.UserContext;
//import com.tianji.promotion.constants.PromotionConstants;
//import com.tianji.promotion.domain.dto.UserCouponDTO;
//import com.tianji.promotion.domain.po.Coupon;
//import com.tianji.promotion.domain.po.ExchangeCode;
//import com.tianji.promotion.domain.po.UserCoupon;
//import com.tianji.promotion.enums.ExchangeCodeStatus;
//import com.tianji.promotion.mapper.CouponMapper;
//import com.tianji.promotion.mapper.UserCouponMapper;
//import com.tianji.promotion.service.IExchangeCodeService;
//import com.tianji.promotion.service.IUserCouponService;
//import com.tianji.promotion.utils.CodeUtil;
//import com.tianji.promotion.utils.MyLock;
//import com.tianji.promotion.utils.MyLockStrategy;
//import com.tianji.promotion.utils.MyLockType;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.redisson.api.RedissonClient;
//import org.springframework.data.redis.core.StringRedisTemplate;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDateTime;
//import java.util.Map;
//
///**
// * <p>
// * 用户领取优惠券的记录，是真正使用的优惠券信息 服务实现类     异步领取优惠券  异步兑换
// * </p>
// *
// * @author sefy
// * @since 2024-05-18
// */
//@Service
//@Slf4j
//@RequiredArgsConstructor
//public class UserCouponRedissonMqAnnotationServiceImpl extends ServiceImpl<UserCouponMapper, UserCoupon> implements IUserCouponService {
//
//    private final CouponMapper couponMapper;
//    private final StringRedisTemplate stringRedisTemplate;
//    private final IExchangeCodeService exchangeCodeService;
//    private final RedissonClient redissonClient;
//    private final RabbitMqHelper mqHelper;
//
//    /**
//     * 领取优惠券
//     *
//     * @param id
//     */
//    @Override
//    //分布式锁 可以对优惠券加锁
//    @MyLock(name = "lock:coupon:uid:#{id}")             //MyLock注解设置了执行顺序为0，所以会在事务开启之前加锁
//    public void receiveCoupon(Long id) {
//        //1.根据id查询优惠券信息 做相关校验
//        if (id == null) {
//            throw new BadRequestException("非法参数");
//        }
////        Coupon coupon = couponMapper.selectById(id);
//        //从redis中获取优惠券信息
//        Coupon coupon = queryCouponByCache(id);
//        if (coupon == null) {
//            throw new BadRequestException("该优惠券不存在");
//        }
////        if (coupon.getStatus() != CouponStatus.ISSUING) {
////            throw new BadRequestException("该优惠券未发放");
////        }
//        LocalDateTime now = LocalDateTime.now();
//        if (now.isBefore(coupon.getIssueBeginTime()) || now.isAfter(coupon.getIssueEndTime())) {
//            throw new BadRequestException("该优惠券已过期或未开始发放");
//        }
////        if (coupon.getTotalNum() <= 0 || coupon.getIssueNum() >= coupon.getTotalNum()) {
////            throw new BadRequestException("该优惠券库存不足");
////        }
//        if (coupon.getTotalNum() <= 0) {
//            throw new BadRequestException("该优惠券库存不足");
//        }
//        //获取该优惠券当前登录用户已领取数量  user_coupon 条件：userId couponId
//        Long userId = UserContext.getUser();
//        /*Integer count = this.lambdaQuery()
//                .eq(UserCoupon::getUserId, userId)
//                .eq(UserCoupon::getId, id)
//                .count();
//        if (count != null && count >= coupon.getUserLimit()) {
//            throw new BadRequestException("已达到领取上限");
//        }
//
//        //2.优惠券的已发送数量+1
//        couponMapper.incrIssNum(id);  //采用这种方法，考虑并发控制，后期仍需要修改
//
//        //3.生成用户券
//        saveUserCoupon(userId, coupon);*/
///*//        2.  1.校验是否超出优惠券的每个人限领取数量 和 2.生成用户券 和 3.优惠券的已发放数量 + 1
//        //基于synchronized的悲观锁
//        synchronized (userId.toString().intern()) {
//            //从aop中上下文中 获取当前代理类对象
//            IUserCouponService userCouponServiceProxy = (IUserCouponService) AopContext.currentProxy();
////            checkAndCreateUserCoupon(userId, coupon, null);  //这种写法是调用原对象的方法
//            userCouponServiceProxy.checkAndCreateUserCoupon(userId, coupon, null);   //这种写法是调用该代理对象的方法，方法是有事务处理的
//        }*/
//
///*        //通过工具类实现分布式锁
//        String key = "lock:coupon:uid:" + userId;
//        RedisLock redisLock = new RedisLock(key, stringRedisTemplate);
//        try {
//            boolean isLock = redisLock.tryLock(5, TimeUnit.SECONDS);
//            if (!isLock) {
//                throw new BizIllegalException("操作太频繁了");
//            }
//            //从aop中上下文中 获取当前代理类对象
//            IUserCouponService userCouponServiceProxy = (IUserCouponService) AopContext.currentProxy();
//            userCouponServiceProxy.checkAndCreateUserCoupon(userId, coupon, null);   //这种写法是调用该代理对象的方法，方法是有事务处理的
//        } finally {
//            redisLock.unlock();
//        }*/
//
//        /*//通过redisson实现分布式锁
//        String key = "lock:coupon:uid:" + userId;
//        RLock lock = redissonClient.getLock(key);
//        try {
//            boolean isLock = lock.tryLock();   //看门口机制会失效，默认失效时间为30秒
//            if (!isLock) {
//                throw new BizIllegalException("操作太频繁了");
//            }
//            //从aop中上下文中 获取当前代理类对象
//            IUserCouponService userCouponServiceProxy = (IUserCouponService) AopContext.currentProxy();
//            userCouponServiceProxy.checkAndCreateUserCoupon(userId, coupon, null);   //这种写法是调用该代理对象的方法，方法是有事务处理的
//        } finally {
//            lock.unlock();
//        }*/
//
//       /* //从aop中上下文中 获取当前代理类对象
//        IUserCouponService userCouponServiceProxy = (IUserCouponService) AopContext.currentProxy();
//        userCouponServiceProxy.checkAndCreateUserCoupon(userId, coupon, null);   //这种写法是调用该代理对象的方法，方法是有事务处理的*/
//
//        //统计已经领取的数量
//        String key = PromotionConstants.USER_COUPON_CACHE_KEY_PREFIX + id;
//        //先直接操作redis将当前用户该优惠券的领取数量+1，increment代表本次领取后的 已领取数量
//        Long increment = stringRedisTemplate.opsForHash().increment(key, userId.toString(), 1);
//
//        //校验是否超过限定数量
//        //如果+1之后不大于限领数量，则可以领取，否则抛异常
//        if (increment > coupon.getUserLimit()) {    //increment代表本次领取后的 已领取数量  所以判断应该为 >
//            throw new BizIllegalException("超出限领数量");
//        }
//
//        //修改优惠券的库存 -1  redis
//        String couponKey = PromotionConstants.COUPON_CACHE_KEY_PREFIX + id;
//        stringRedisTemplate.opsForHash().increment(couponKey, "totalNum", -1);
//
//        //发送消息到mq 消息的内容 userId  couponId
//        UserCouponDTO msg = new UserCouponDTO();
//        msg.setCouponId(id);
//        msg.setUserId(userId);
//        mqHelper.send(MqConstants.Exchange.PROMOTION_EXCHANGE, MqConstants.Key.COUPON_RECEIVE, msg);
//    }
//
//    /**
//     * 从redis中获取优惠券时间（优惠券id、领券的开始时间和结束时间、发行的总数量、限领数量）
//     *
//     * @param id
//     * @return
//     */
//    private Coupon queryCouponByCache(Long id) {
//        //1.拼接key
//        String key = PromotionConstants.COUPON_CACHE_KEY_PREFIX + id;
//        //2.从redis中获取数据
//        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
//        Coupon coupon = BeanUtils.mapToBean(entries, Coupon.class, false, CopyOptions.create());
//        return coupon;
//    }
//
//
//    /**
//     * 校验是否超出优惠券的每个人限领取数量 和 优惠券的已发放数量 + 1 和 生成用户券
//     * 第一步：查询当前用户对于该优惠券的已领取数量
//     * 第二步：判断是否超出限领数量
//     * 第三步：如果没有超出限领数量，则更新该优惠券的已发放数量 +1  （采用乐观锁解决超卖问题）
//     * 第四步：如果没有超出限领数量，则生成用户券
//     *
//     * @param userId
//     * @param coupon
//     * @param exchangeCodeId
//     */
//    @Transactional
//    @MyLock(name = "lock:coupon:uid:#{userId}",
//            lockType = MyLockType.RE_ENTRANT_LOCK,
//            lockStrategy = MyLockStrategy.FAIL_AFTER_RETRY_TIMEOUT)             //MyLock注解设置了执行顺序为0，所以会在事务开启之前加锁
//    public void checkAndCreateUserCoupon(Long userId, Coupon coupon, Long exchangeCodeId) {
//        //Long类型  -128~ 127之间 是同一个对象
//        //Long.toString 方法底层是new String对象，所以还是不同对象
//        //Long.toString().intern intern方法是强制从常量池中取字符串
//
//        //1.获取该优惠券当前登录用户已领取数量  user_coupon 条件：userId couponId
//        //synchronized (userId.toString().intern())
//        Integer count = this.lambdaQuery()
//                .eq(UserCoupon::getUserId, userId)
//                .eq(UserCoupon::getCouponId, coupon.getId())
//                .count();
//        if (count != null && count >= coupon.getUserLimit()) {
//            throw new BadRequestException("已达到领取上限");
//        }
//
//        //2.优惠券的已发送数量+1    这里采用乐观锁解决超卖问题
//        int result = couponMapper.incrIssNum(coupon.getId());//采用这种方法，考虑并发控制，后期仍需要修改
//        if (result == 0) {
//            //更新失败，说明库存不足（已经领取完）
//            throw new BizIllegalException("优惠券库存不足！");
//        }
//
//        //3.生成用户券
//        saveUserCoupon(userId, coupon);
//
//        //4.更新兑换码的状态   db
//        if (exchangeCodeId != null) {
//            exchangeCodeService.lambdaUpdate()
//                    .set(ExchangeCode::getStatus, ExchangeCodeStatus.USED)
//                    .set(ExchangeCode::getUserId, userId)
//                    .eq(ExchangeCode::getId, exchangeCodeId)
//                    .update();
//        }
//    }
//
//
//    //    @MyLock(name = "lock:coupon:uid:#{userId}",
////            lockType = MyLockType.RE_ENTRANT_LOCK,
////            lockStrategy = MyLockStrategy.FAIL_AFTER_RETRY_TIMEOUT)             //MyLock注解设置了执行顺序为0，所以会在事务开启之前加锁
//    @Transactional
//    @Override
//    public void checkAndCreateUserCouponNew(UserCouponDTO msg) {
//        //Long类型  -128~ 127之间 是同一个对象
//        //Long.toString 方法底层是new String对象，所以还是不同对象
//        //Long.toString().intern intern方法是强制从常量池中取字符串
//
//        //1.获取该优惠券当前登录用户已领取数量  user_coupon 条件：userId couponId
//        //synchronized (userId.toString().intern())
////        Integer count = this.lambdaQuery()
////                .eq(UserCoupon::getUserId, userId)
////                .eq(UserCoupon::getCouponId, coupon.getId())
////                .count();
////        if (count != null && count >= coupon.getUserLimit()) {
////            throw new BadRequestException("已达到领取上限");
////        }
//        //从db中查询优惠券信息
//        Coupon coupon = couponMapper.selectById(msg.getCouponId());
//        if (coupon == null) {
//            return;
//        }
//        //2.优惠券的已发送数量+1    这里采用乐观锁解决超卖问题
//        int result = couponMapper.incrIssNum(coupon.getId());//采用这种方法，考虑并发控制，后期仍需要修改
//        if (result == 0) {
//            //更新失败，说明库存不足（已经领取完）
//            return;
//        }
//
//        //3.生成用户券
//        saveUserCoupon(msg.getUserId(), coupon);
//
//        //4.更新兑换码的状态   db
//        if (msg.getExchangeCodeId() != null) {
//            exchangeCodeService.lambdaUpdate()
//                    .set(ExchangeCode::getStatus, ExchangeCodeStatus.USED)
//                    .set(ExchangeCode::getUserId, msg.getUserId())
//                    .eq(ExchangeCode::getId, msg.getExchangeCodeId())
//                    .update();
//        }
//    }
//
//    /**
//     * 保存用户券
//     *
//     * @param userId
//     * @param coupon
//     */
//    private void saveUserCoupon(Long userId, Coupon coupon) {
//        UserCoupon userCoupon = new UserCoupon();
//        userCoupon.setUserId(userId);
//        userCoupon.setCouponId(coupon.getId());
//
//        LocalDateTime termBeginTime = coupon.getTermBeginTime();  //该优惠券使用开始时间
//        LocalDateTime termEndTime = coupon.getTermEndTime();        //该优惠券使用截至时间
//        if (termEndTime == null && termEndTime == null) {
//            termBeginTime = LocalDateTime.now();
//            termEndTime = termBeginTime.plusDays(coupon.getTermDays());
//        }
//        userCoupon.setTermBeginTime(termBeginTime);
//        userCoupon.setTermEndTime(termEndTime);
//        this.save(userCoupon);
//    }
//
//    /**
//     * 兑换码兑换优惠券
//     * 为什么4-9步要使用try-catch语句包括进去：如果不使用，家人当前这个兑换码未兑换，
//     * 在第三步里会将bitmap中该兑换码改为已兑换，二如果4-9步之间出现了异常，而redis的操作不会回滚。
//     * 并且再捕获异常后要重置reids bitmap该兑换码的状态
//     *
//     * @param code
//     */
//    @Override
//    @Transactional
//    public void exchangeCoupon(String code) {
//        //1.校验code是否为空
//        if (code == null) {
//            throw new BadRequestException("非法参数");
//        }
//
//        //2.解析兑换码得到 自增id
//        long exchangeCodeId = CodeUtil.parseCode(code);
//        log.debug("兑换码自增id：{}", exchangeCodeId);
//        //3.判断是否已经兑换 通过redis的bitmap来判断   setbit key offset 1 返回true代表已经兑换
//        boolean result = exchangeCodeService.updateExchangeCodeMark(exchangeCodeId, true);
//        if (result) {
//            //说明该兑换码已经兑换过
//            throw new BizIllegalException("兑换码已被使用");
//        }
//
//        try {
////            //4.判断兑换码是否存在于exchange_code表里 （根据自增id）
////            ExchangeCode exchangeCode = exchangeCodeService.getById(exchangeCodeId);
//            Long couponId = exchangeCodeService.exchangeTargetId(exchangeCodeId);
//            if (couponId == null) {
//                throw new BizIllegalException("兑换码不存在");
//            }
//            //查询要兑换的优惠券 从redis中查询
//            Coupon coupon = queryCouponByCache(couponId);
//
//            //5.判断是否过期
//            LocalDateTime now = LocalDateTime.now();
//            if (now.isAfter(coupon.getIssueEndTime())) {
//                throw new BizIllegalException("兑换码已过期");
//            }
//            if (now.isBefore(coupon.getIssueBeginTime())){
//                throw new BizIllegalException("活动尚未开始");
//            }
//
//            Long userId = UserContext.getUser();
//            //6.查询领取数量
//            String key = PromotionConstants.USER_COUPON_CACHE_KEY_PREFIX + couponId;
//            Long count = stringRedisTemplate.opsForHash().increment(key, userId.toString(), 1);
//            if (count > coupon.getUserLimit()){
//                throw new BadRequestException("超出限领数量");
//            }
//
//            //7.发送mq消息
//            UserCouponDTO uc = new UserCouponDTO();
//            uc.setUserId(userId);
//            uc.setCouponId(couponId);
//            uc.setExchangeCodeId((int) exchangeCodeId);
//            mqHelper.send(MqConstants.Exchange.PROMOTION_EXCHANGE, MqConstants.Key.COUPON_RECEIVE, uc);
//
//        } catch (Exception e) {
//            //10.将兑换码的状态重置为 未兑换
//            exchangeCodeService.updateExchangeCodeMark(exchangeCodeId, false);
//            throw e;
//        }
//
//    }
//
//}
