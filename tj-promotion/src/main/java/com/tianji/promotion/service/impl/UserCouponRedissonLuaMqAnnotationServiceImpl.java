package com.tianji.promotion.service.impl;

import cn.hutool.core.bean.copier.CopyOptions;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.autoconfigure.mq.RocketMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.NumberUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.promotion.constants.PromotionConstants;
import com.tianji.promotion.discount.Discount;
import com.tianji.promotion.discount.DiscountStrategy;
import com.tianji.promotion.domain.dto.CouponDiscountDTO;
import com.tianji.promotion.domain.dto.OrderCourseDTO;
import com.tianji.promotion.domain.dto.UserCouponDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.CouponScope;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.tianji.promotion.domain.po.UserCoupon;
import com.tianji.promotion.dubbo.client.CouponStockClient;
import com.tianji.promotion.enums.ExchangeCodeStatus;
import com.tianji.promotion.mapper.CouponMapper;
import com.tianji.promotion.mapper.UserCouponMapper;
import com.tianji.promotion.service.ICouponScopeService;
import com.tianji.promotion.service.IExchangeCodeService;
import com.tianji.promotion.service.IUserCouponService;
import com.tianji.promotion.utils.*;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tianji.promotion.constants.PromotionConstants.COUPON_CODE_MAP_KEY;
import static com.tianji.promotion.constants.PromotionConstants.COUPON_RANGE_KEY;
import static org.codehaus.groovy.runtime.DefaultGroovyMethods.*;

/**
 * <p>
 * 用户领取优惠券的记录，是真正使用的优惠券信息 服务实现类     异步领取
 * </p>
 *
 * @author sefy
 * @since 2024-05-18
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserCouponRedissonLuaMqAnnotationServiceImpl extends ServiceImpl<UserCouponMapper, UserCoupon> implements IUserCouponService {

    private final CouponMapper couponMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final IExchangeCodeService exchangeCodeService;
    private final RedissonClient redissonClient;
    private final RabbitMqHelper mqHelper;
    private final ICouponScopeService couponScopeService;
    private final Executor calculateSolutionExecutor;

    @Autowired
    private CouponStockClient couponStockClient;

    private static final RedisScript<Long> RECEIVE_COUPON_SCRIPT;
    private static final RedisScript<String> EXCHANGE_COUPON_SCRIPT;

    static {
        RECEIVE_COUPON_SCRIPT = RedisScript.of(new ClassPathResource("lua/receive_coupon.lua"), Long.class);
        EXCHANGE_COUPON_SCRIPT = RedisScript.of(new ClassPathResource("lua/exchange_coupon.lua"), String.class);
    }

    private final RocketMqHelper rocketMqHelper;

    /**
     * 根据shopId 和 id领取优惠券
     *
     * @param shopId 商家id（creater）
     * @param id     优惠券id
     */
    @Override
    public void receiveCouponByShopIdAndId(Long shopId, Long id) {
        //先判断缓存是否存在（缓存穿透缓存击穿）TODO

        // 1.执行LUA脚本，判断结果
        // 1.1.准备参数
        String key1 = String.format(PromotionConstants.SHOP_COUPON_CHECK_KEY_FORMAT, shopId, id);
        String key2 = String.format(PromotionConstants.SHOP_COUPON_ISSUED_KEY_FORMAT, shopId, id);
        Long userId = UserContext.getUser();
        // 1.2.执行脚本
        Long r = stringRedisTemplate.execute(RECEIVE_COUPON_SCRIPT, List.of(key1, key2), userId.toString());
        int result = NumberUtils.null2Zero(r).intValue();
        if (result != 0) {
            // 结果大于0，说明出现异常
            throw new BizIllegalException(PromotionConstants.RECEIVE_COUPON_ERROR_MSG[result - 1]);
        }

        //2.发送消息到mq 消息的内容 userId  couponId creater
        UserCouponDTO userCoupon = new UserCouponDTO();
        userCoupon.setCouponId(id);
        userCoupon.setCreater(shopId);
        userCoupon.setUserId(userId);
        boolean isSuccess = rocketMqHelper.sendSync(MqConstants.Topic.PROMOTION_TOPIC, userCoupon);
        log.info("发送领取优惠券消息到mq，是否成功：{}", isSuccess);
    }


    /**
     * 领取优惠券
     *（暂不用）
     * @param id
     */
    @Override
    public void receiveCoupon(Long id) {
        // 1.执行LUA脚本，判断结果
        // 1.1.准备参数
//        String key1 = String.format(PromotionConstants.SHOP_COUPON_CHECK_KEY_FORMAT, shopId, id);
//        String key2 = String.format(PromotionConstants.SHOP_COUPON_ISSUED_KEY_FORMAT, shopId, id);
        Long userId = UserContext.getUser();
        // 1.2.执行脚本
        Long r = stringRedisTemplate.execute(RECEIVE_COUPON_SCRIPT, List.of("1", "2"), userId.toString());
        int result = NumberUtils.null2Zero(r).intValue();
        if (result != 0) {
            // 结果大于0，说明出现异常
            throw new BizIllegalException(PromotionConstants.RECEIVE_COUPON_ERROR_MSG[result - 1]);
        }

        //2.发送消息到mq 消息的内容 userId  couponId
        UserCouponDTO userCoupon = new UserCouponDTO();
        userCoupon.setCouponId(id);
        userCoupon.setUserId(userId);
        boolean isSuccess = rocketMqHelper.sendSync(MqConstants.Topic.PROMOTION_TOPIC, userCoupon);
        log.info("发送领取优惠券消息到mq，是否成功：{}", isSuccess);
    }

    /**
     * 从redis中获取优惠券时间（优惠券id、领券的开始时间和结束时间、发行的总数量、限领数量）
     *
     * @param id
     * @return
     */
    private Coupon queryCouponByCache(Long id) {
        //1.拼接key
        String key = PromotionConstants.COUPON_CACHE_KEY_PREFIX + id;
        //2.从redis中获取数据
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
        return BeanUtils.mapToBean(entries, Coupon.class, false, CopyOptions.create());
    }


//    /**
//     * 校验是否超出优惠券的每个人限领取数量 和 优惠券的已发放数量 + 1 和 生成用户券
//     * 第一步：查询当前用户对于该优惠券的已领取数量
//     * 第二步：判断是否超出限领数量
//     * 第三步：如果没有超出限领数量，则更新该优惠券的已发放数量 +1  （采用乐观锁解决超卖问题）
//     * 第四步：如果没有超出限领数量，则生成用户券
//     * 这个方法在这里没用到
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
//
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


    //    @MyLock(name = "lock:coupon:uid:#{userId}",
//            lockType = MyLockType.RE_ENTRANT_LOCK,
//            lockStrategy = MyLockStrategy.FAIL_AFTER_RETRY_TIMEOUT)             //MyLock注解设置了执行顺序为0，所以会在事务开启之前加锁
    @Override
//    @GlobalTransactional(rollbackFor = Exception.class)  //应该采用分布式事务的，但是由于sharding-Sphere和seata存在bug，暂时注释掉
    public void checkAndCreateUserCouponNew(UserCouponDTO msg) {

        //1.从db中查询优惠券基本信息
        Coupon coupon = couponMapper.selectByCreaterAndIdCoupon(msg.getCreater(), msg.getCouponId());
        if (coupon == null) {
            //优惠券不存在
            throw new BizIllegalException("优惠券不存在！");
        }

        //2.远程调用stock服务的rpc接口扣减优惠券的库存    这里采用乐观锁解决超卖问题
        int result = couponStockClient.increaseInssueNum(msg.getCreater(), msg.getCouponId());
        if (result == 0) {
            //更新失败，说明库存不足（已经领取完）
            throw new BizIllegalException("库存不足！");
        }

        //3.生成用户券
        saveUserCoupon(msg.getUserId(), coupon);

        //4.更新兑换码的状态   db
        if (msg.getExchangeCodeId() != null) {
            exchangeCodeService.lambdaUpdate()
                    .set(ExchangeCode::getStatus, ExchangeCodeStatus.USED)
                    .set(ExchangeCode::getUserId, msg.getUserId())
                    .eq(ExchangeCode::getId, msg.getExchangeCodeId())
                    .update();
        }
    }

    /**
     * 查询我的可用优惠券方案
     *
     * @param orderCourses
     * @return
     */
    @Override
    public List<CouponDiscountDTO> findDiscountSolution(List<OrderCourseDTO> orderCourses) {
        //1.查询当前用户可用的优惠券 coupon表 和 user_coupon表  条件：userId status=1 查哪些字段 优惠券的规则 优惠券id 用户券id
        List<Coupon> coupons = getBaseMapper().queryMyCoupon(UserContext.getUser());
        if (CollUtils.isEmpty(coupons)) {
            return CollUtils.emptyList();   //没有可用优惠券
        }

/*        log.debug("用户的可用优惠券总共有{}张", coupons);
        for (Coupon coupon : coupons) {
            log.debug("优惠券：{}， {}", DiscountStrategy.getDiscount(coupon.getDiscountType()).getRule(coupon),
                    coupon);
        }*/

        //2.初筛
        //2.1.计算订单中课程的总金额 对course的price累加
        int totalAmount = orderCourses.stream().mapToInt(OrderCourseDTO::getPrice).sum();
        log.debug("订单的总金额：{}", totalAmount);
        //2.2.校验优惠券是否满足使用门槛
        List<Coupon> availiableCoupons = coupons.stream()
                .filter(coupon -> DiscountStrategy.getDiscount(coupon.getDiscountType()).canUse(totalAmount, coupon))
                .collect(Collectors.toList());
        if (CollUtils.isEmpty(availiableCoupons)) {
            return CollUtils.emptyList();
        }

/*        log.debug("经过初筛之后，还有{}张", availiableCoupons.size());
        for (Coupon coupon : availiableCoupons) {
            log.debug("优惠券：{}， {}", DiscountStrategy.getDiscount(coupon.getDiscountType()).getRule(coupon),
                    coupon);
        }*/

        //3.细筛（需要考虑优惠券的限定范围） 排列组合
        Map<Coupon, List<OrderCourseDTO>> avaMap = findAvailableCoupons(availiableCoupons, orderCourses);
        if (avaMap.isEmpty()) {
            return CollUtils.emptyList();   //没有可用优惠券
        }
/*        Set<Map.Entry<Coupon, List<OrderCourseDTO>>> entries = avaMap.entrySet();
        for (Map.Entry<Coupon, List<OrderCourseDTO>> entry : entries) {
            log.debug("细筛之优惠券：{} {}",
                    DiscountStrategy.getDiscount(entry.getKey().getDiscountType()).getRule(entry.getKey()),
                    entry.getKey());
            List<OrderCourseDTO> value = entry.getValue();
            for (OrderCourseDTO courseDTO : value) {
                log.debug("可用课程：{}", courseDTO);
            }
        }*/
        availiableCoupons = new ArrayList<>(avaMap.keySet());  //这时候才是真正可用的优惠券集合
/*        log.debug("经过细筛之后，还有{}张", availiableCoupons.size());
        for (Coupon coupon : availiableCoupons) {
            log.debug("优惠券：{}， {}", DiscountStrategy.getDiscount(coupon.getDiscountType()).getRule(coupon),
                    coupon);
        }*/
        List<List<Coupon>> solutionList = PermuteUtil.permute(availiableCoupons);  //排列组合计算方案
        for (Coupon availiableCoupon : availiableCoupons) {
            solutionList.add(List.of(availiableCoupon));  // 单张券的方案也加入
        }
/*        log.debug("排列组合");
        for (List<Coupon> solution : solutionList) {
            List<Long> cids = solution.stream().map(Coupon::getId).collect(Collectors.toList());
            log.debug("{}", cids);
        }*/

/*        //4.计算每一种组合的优惠金额
        log.debug("开始计算每一种组合的优惠明细");
        List<CouponDiscountDTO> dtos = new ArrayList<>();
        for (List<Coupon> solution : solutionList) {
            CouponDiscountDTO dto = calculateSolutionDiscount(avaMap, orderCourses, solution);
            //log.debug("方案最终优惠：{} 方案中使用的优惠券id：{}  规则{}", dto.getDiscountAmount(), dto.getIds(), dto.getRules());
            dtos.add(dto);
        }*/

        //5.使用多线程改造第4步  并行计算每一种组合的优惠明细
        log.debug("使用多线程并行计算每一种组合的优惠明细");
//        List<CouponDiscountDTO> dtos = new ArrayList<>();   //ArrayList线程不安全
        List<CouponDiscountDTO> dtos = Collections.synchronizedList(new ArrayList<>());  //synchronizedList线程安全
        CountDownLatch latch = new CountDownLatch(solutionList.size());   //计数器的初始值就是优惠券组合数的个数
        for (List<Coupon> solution : solutionList) {
            CompletableFuture.supplyAsync(() -> {
                //log.debug("线程{}开始计算", Thread.currentThread().getName());
                CouponDiscountDTO dto = calculateSolutionDiscount(avaMap, orderCourses, solution);
                return dto;
            }, calculateSolutionExecutor).thenAccept(dto -> {
                //log.debug("方案最终优惠：{} 方案中使用的优惠券id：{}  规则{}", dto.getDiscountAmount(), dto.getIds(), dto.getRules());
                dtos.add(dto);
                latch.countDown();  //计数器-1
            });
        }
        try {
            latch.await(2, TimeUnit.SECONDS);   //主线程会最多阻塞2秒
        } catch (InterruptedException e) {
            log.error("多线程并行计算组合优惠明细出错！", e);
        }

        //6.筛选最优方案
        return findBestSolution(dtos);
    }

    //查看本机的cpu核数
//    public static void main(String[] args) {
//        System.out.println(Runtime.getRuntime().availableProcessors());
//    }

    /**
     * 求最优解：
     * - 用券相同时，优惠金额最高的方案
     * - 优惠金额相同时，用券最少的方案
     *
     * @param solutions
     * @return
     */
    private List<CouponDiscountDTO> findBestSolution(List<CouponDiscountDTO> solutions) {
        //1.创建两个map分别 用券数量相同，优惠金额最高  金额最高，用券最少
        Map<String, CouponDiscountDTO> moreDiscountMap = new HashMap<>();
        Map<Integer, CouponDiscountDTO> lessCouponMap = new HashMap<>();
        //2.循环方案 向map记录
        for (CouponDiscountDTO solution : solutions) {
            //2.1.对优惠劵id集合先升序排序 再转字符串 以逗号拼接
            String ids = solution.getIds().stream()
                    .sorted(Comparator.comparing(Long::longValue))
                    .map(String::valueOf)   //转String
                    .collect(Collectors.joining(","));//逗号拼接
            //2.2.从moreDiscountMap 取旧的记录 判断：如果当前方案的优惠金额小于旧的 忽略当前  否则 覆盖旧的方案
            CouponDiscountDTO old = moreDiscountMap.get(ids);
            if (old != null && old.getDiscountAmount() >= solution.getDiscountAmount()) {
                continue;
            }
            //2.3.从lessCouponMap 取旧的记录 判断：如果当前方案的用券数量大于旧的 忽略当前  否则 覆盖旧的方案
            old = lessCouponMap.get(solution.getDiscountAmount());
            int newSize = solution.getIds().size();   //新的方案的用券数量
            if (old != null && newSize > 1 && old.getIds().size() <= newSize) {
                continue;
            }
            //2.4当前方案更优，添加方案到map中
            moreDiscountMap.put(ids, solution);
            lessCouponMap.put(solution.getDiscountAmount(), solution);
        }

        //3.求两个map的交集
        Collection<CouponDiscountDTO> bestSolution = CollUtils.intersection(moreDiscountMap.values(), lessCouponMap.values());

        //4.对最终的方案结果 按优惠金额 倒序排列
        List<CouponDiscountDTO> finalSolutions = bestSolution.stream().sorted(Comparator.comparing(CouponDiscountDTO::getDiscountAmount).reversed()).collect(Collectors.toList());
        return finalSolutions;

    }

    /**
     * 计算某个方案的优惠明细
     *
     * @param avaMap       优惠券和可用课程的映射集合
     * @param orderCourses 订单中所有的课程
     * @param solution     方案
     * @return
     */
    private CouponDiscountDTO calculateSolutionDiscount(Map<Coupon, List<OrderCourseDTO>> avaMap, List<OrderCourseDTO> orderCourses, List<Coupon> solution) {
        //1.创建方案结果dto对象
        CouponDiscountDTO dto = new CouponDiscountDTO();
        //2.初始化商品id和商品折扣金额明细的映射，初始折扣金额都为0    detailMap  key：商品id  value：初始都为0
        Map<Long, Integer> detailMap = orderCourses.stream().collect(Collectors.toMap(OrderCourseDTO::getId, orderCourseDTO -> 0));
        //3.计算该方案的折扣明细
        //3.1.循环所有的优惠券
        for (Coupon coupon : solution) {
            //3.2.取出该优惠券对应的可用课程
            List<OrderCourseDTO> availiableCourses = avaMap.get(coupon);
            //3.3.可算可用课程的总金额 sum（商品价格-商品折扣明细）
            int totalAmount = availiableCourses.stream().mapToInt(value -> value.getPrice() - detailMap.get(value.getId())).sum();
            //3.4.判断该优惠券是否可用
            Discount discount = DiscountStrategy.getDiscount(coupon.getDiscountType());
            if (!discount.canUse(totalAmount, coupon)) {
                continue;  //如果该优惠券不可以使用，则跳到下一张优惠券
            }

            //3.5.如果可用使用，计算该优惠券使用后的折扣值
            int discountAmount = discount.calculateDiscount(totalAmount, coupon);

            //3.6.更新商品的折扣明细（更新商品id对应的折扣明细）
            calculateDetailDisCount(totalAmount, availiableCourses, discountAmount, detailMap);

            //3.7.累加每一个优惠券的优惠金额  赋值给赋值方案结果的dto对象
            dto.getIds().add(coupon.getId());
            dto.getRules().add(discount.getRule(coupon));
            dto.setDiscountAmount(dto.getDiscountAmount() + discountAmount);// 所有生效的优惠券累加折扣
        }
        return dto;
    }

    /**
     * @param totalAmount       可用的课程总金额
     * @param availiableCourses 当前优惠券可用的课程集合
     * @param discountAmount    当前优惠券能优惠的金额
     * @param detailMap         商品id和商品折扣金额明细的映射
     */
    private void calculateDetailDisCount(int totalAmount, List<OrderCourseDTO> availiableCourses, int discountAmount, Map<Long, Integer> detailMap) {
        int remainDiscount = discountAmount;
        int detail = 0;
        for (int i = 0; i < availiableCourses.size(); i++) {
            OrderCourseDTO courseDTO = availiableCourses.get(i);
            if (i == availiableCourses.size() - 1) {
                detail = remainDiscount;
            } else {
                detail = courseDTO.getPrice() * discountAmount / totalAmount;
                remainDiscount -= detail;
            }
            //更新折扣明细
            detailMap.put(courseDTO.getId(), detailMap.get(courseDTO.getId()) + detail);
        }
    }

    /**
     * 细筛，查询每一个优惠券 对应的可用课程
     *
     * @param coupons      初筛之后的优惠券组合
     * @param orderCourses 订单中的课程集合
     * @return
     */
    private Map<Coupon, List<OrderCourseDTO>> findAvailableCoupons(List<Coupon> coupons, List<OrderCourseDTO> orderCourses) {
        Map<Coupon, List<OrderCourseDTO>> avaMap = new HashMap<>();  //key：优惠券  value：订单中可以使用该优惠券的课程
        //1.循环遍历初筛后的优惠券集合
        for (Coupon coupon : coupons) {
            //2.找出每一个优惠券的可用课程
            List<OrderCourseDTO> availableCourses = orderCourses;  //先设置为订单中的所有课程，如果没有指定范围，说明是通用券，则所有课程都可以使用
            //2.1.先判断优惠券是否指定了可用范围
            if (coupon.getSpecific()) {
                //2.2.如果指定了可用范围，需要查询coupon_scope表  条件：couponId
                List<CouponScope> couponScopeList = couponScopeService.lambdaQuery().eq(CouponScope::getCouponId, coupon.getId()).list();
                //2.3.得到该优惠券限定范围内的id集合
                List<Long> scopeIds = couponScopeList.stream().map(CouponScope::getBizId).collect(Collectors.toList());
                //2.4.从 orderCourses中筛选限定范围内的集合
                availableCourses = orderCourses.stream()
                        .filter(orderCourseDTO -> scopeIds.contains(orderCourseDTO.getCateId()))
                        .collect(Collectors.toList());
            }

            if (CollUtils.isEmpty(availableCourses)) {
                continue;   //说明当前优惠券限定了范围，但在订单中的课程没有找到可用课程，说明该优惠券不可用，直接跳到下一张优惠券
            }

            //3.计算该优惠券 可用课程的总金额
            int totalAmount = availableCourses.stream().mapToInt(OrderCourseDTO::getPrice).sum();  //totalAmount代表该优惠券可用课程的总金额

            //4.判断该优惠券是否可用，可用则添加到Map中
            boolean flag = DiscountStrategy.getDiscount(coupon.getDiscountType()).canUse(totalAmount, coupon);
            if (flag) {
                avaMap.put(coupon, availableCourses);
            }
        }
        return avaMap;
    }

    /**
     * 保存用户券
     *
     * @param userId
     * @param coupon
     */
    private void saveUserCoupon(Long userId, Coupon coupon) {
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(coupon.getId());

        LocalDateTime termBeginTime = coupon.getTermBeginTime();  //该优惠券使用开始时间
        LocalDateTime termEndTime = coupon.getTermEndTime();        //该优惠券使用截至时间
        if (termEndTime == null && termEndTime == null) {
            termBeginTime = LocalDateTime.now();
            termEndTime = termBeginTime.plusDays(coupon.getTermDays());
        }
        userCoupon.setTermBeginTime(termBeginTime);
        userCoupon.setTermEndTime(termEndTime);
        this.save(userCoupon);
    }

    /**
     * 兑换码兑换优惠券
     * 为什么4-9步要使用try-catch语句包括进去：如果不使用，家人当前这个兑换码未兑换，
     * 在第三步里会将bitmap中该兑换码改为已兑换，二如果4-9步之间出现了异常，而redis的操作不会回滚。
     * 并且再捕获异常后要重置reids bitmap该兑换码的状态
     *
     * @param code
     */
    @Override
    public void exchangeCoupon(String code) {
        //1.校验code是否为空
        if (code == null) {
            throw new BadRequestException("非法参数");
        }

        //2.解析兑换码得到 自增id
        long exchangeCodeId = CodeUtil.parseCode(code);
        log.debug("兑换码自增id：{}", exchangeCodeId);

        // 3.执行LUA脚本
        Long userId = UserContext.getUser();
        String result = stringRedisTemplate.execute(
                EXCHANGE_COUPON_SCRIPT,
                List.of(COUPON_CODE_MAP_KEY, COUPON_RANGE_KEY),
                String.valueOf(exchangeCodeId), String.valueOf(exchangeCodeId + 5000), userId.toString());
        long r = NumberUtils.parseLong(result);
        if (r < 10) {
            // 异常结果应该是在1~5之间
            throw new BizIllegalException(PromotionConstants.EXCHANGE_COUPON_ERROR_MSG[(int) (r - 1)]);
        }

        //4.发送mq消息
        UserCouponDTO uc = new UserCouponDTO();
        uc.setUserId(userId);
        uc.setCouponId(r);
        uc.setExchangeCodeId((int) exchangeCodeId);
        boolean isSuccess = rocketMqHelper.sendSync(MqConstants.Topic.PROMOTION_TOPIC, uc);
        log.info("发送兑换优惠券消息到mq，是否成功：{}", isSuccess);
        //        mqHelper.send(MqConstants.Exchange.PROMOTION_EXCHANGE, MqConstants.Key.COUPON_RECEIVE, uc);
    }


}
