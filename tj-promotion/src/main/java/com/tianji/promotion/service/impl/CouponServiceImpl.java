package com.tianji.promotion.service.impl;

import cn.hutool.core.lang.Pair;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.interfaces.stock.StockDubboService;
import com.tianji.common.cache.distribute.DistributedCacheService;
import com.tianji.common.domain.cache.CouponBusinessCache;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.*;
import com.tianji.promotion.cache.CouponCacheService;
import com.tianji.promotion.cache.CouponListCacheService;
import com.tianji.promotion.constants.PromotionConstants;
import com.tianji.promotion.domain.dto.CouponFormDTO;
import com.tianji.promotion.domain.dto.CouponIssueFormDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.CouponScope;
import com.tianji.promotion.domain.po.UserCoupon;
import com.tianji.promotion.domain.query.CouponQuery;
import com.tianji.promotion.domain.vo.CouponCacheVO;
import com.tianji.promotion.domain.vo.CouponPageVO;
import com.tianji.promotion.domain.vo.CouponVO;
import com.tianji.promotion.dubbo.client.CouponStockClient;
import com.tianji.promotion.enums.CouponStatus;
import com.tianji.promotion.enums.ObtainType;
import com.tianji.promotion.enums.UserCouponStatus;
import com.tianji.promotion.mapper.CouponMapper;
import com.tianji.promotion.service.ICouponScopeService;
import com.tianji.promotion.service.ICouponService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.promotion.service.IExchangeCodeService;
import com.tianji.promotion.service.IUserCouponService;
import io.seata.spring.annotation.GlobalTransactional;
import io.swagger.models.auth.In;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.rpc.RpcException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * 优惠券的规则信息 服务实现类
 * </p>
 *
 * @author sefy
 * @since 2024-05-15
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CouponServiceImpl extends ServiceImpl<CouponMapper, Coupon> implements ICouponService {

    private final ICouponScopeService couponScopeService;
    private final IExchangeCodeService exchangeCodeService;
    private final IUserCouponService userCouponService;

    @Autowired
    private DistributedCacheService distributedCacheService;
    @Autowired
    private CouponStockClient couponStockClient;
    @Autowired
    private CouponCacheService couponCacheService;
    @Autowired
    private CouponListCacheService couponListCacheService;

    /**
     * 新增优惠券
     *
     * @param dto
     */
    @Override
//    @GlobalTransactional(rollbackFor = Exception.class)   //涉及对多个数据库的插入操作，加分布式事务，但由于ShardingSphere 5.X 与seata整合存在bug，所以这里先注释掉
    public void saveCoupon(CouponFormDTO dto) {
        //1.dto转po保存优惠券 coupon表
        Coupon coupon = BeanUtils.copyBean(dto, Coupon.class);
        this.save(coupon);

        //2.远程调用库存服务的 设置优惠券库存
        try {
            couponStockClient.setCouponStock(coupon.getId(), dto.getTotalNum(), UserContext.getUser());
        } catch (Exception e) {
            log.error("远程调用库存服务失败", e);
            throw e;
        }

        //3.判断是否限定了范围 specific == false 说明无限定范围直接返回
        if (!dto.getSpecific()) {
            return;
        }

        //4.specific == true  需要校验scopes是否为空
        List<Long> scopes = dto.getScopes();
        if (CollUtils.isEmpty(scopes)) {
            throw new BizIllegalException("分类id不能为空");
        }

        //5.保存优惠券的限定范围  coupon_scope
        List<CouponScope> csList = scopes.stream().map(aLong -> new CouponScope().setCouponId(coupon.getId()).setType(1).setBizId(aLong)).collect(Collectors.toList());
        couponScopeService.saveBatch(csList);
    }

    /**
     * 分页查询优惠券列表-管理端
     *
     * @param query
     * @return
     */
    @Override
    public PageDTO<CouponPageVO> queryCouponPage(CouponQuery query) {

        //获取当前登录用户
        Long userId = UserContext.getUser();
        if (userId == null) {
            throw new BadRequestException("用户需要登录才能查询");
        }

        //1.分页条件查询查询优惠券表 Coupon
        Page<Coupon> page = this.lambdaQuery().eq(query.getType() != null, Coupon::getDiscountType, query.getType()).eq(query.getStatus() != null, Coupon::getStatus, query.getStatus()).eq(Coupon::getCreater, userId).like(StringUtils.isNotBlank(query.getName()), Coupon::getName, query.getName()).page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<Coupon> coupons = page.getRecords();
        if (CollUtils.isEmpty(coupons)) {
            return PageDTO.empty(page);
        }

        //2.封装vo返回
        List<CouponPageVO> voList = BeanUtils.copyList(coupons, CouponPageVO.class);

        //3.
        return PageDTO.of(page, voList);
    }

    /**
     * 发放优惠券
     *
     * @param id
     * @param dto
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void issueCoupon(Long id, CouponIssueFormDTO dto) {
        log.debug("发放优惠券，线程名：{}", Thread.currentThread().getName());  //Tomcat默认线程
        //1.校验id
        if (id == null || !id.equals(dto.getId())) {
            throw new BadRequestException("非法参数");
        }

        //2.校验优惠券id是否存在
        Coupon coupon = this.getById(id);
        if (coupon == null) {
            throw new BadRequestException("优惠券不存在");
        }

        //3.校验优惠券状态 只有为未发放和暂停状态才能发放
        if (coupon.getStatus() != CouponStatus.DRAFT && coupon.getStatus() != CouponStatus.PAUSE) {
            throw new BizIllegalException("只有待发放和暂停中的优惠券才能发放");
        }

        //4.修改优惠券的 领取开始和结束日期 使用有效期开始和结束日期 天数 状态
        LocalDateTime now = LocalDateTime.now();
        //isBeginIssue代表是否立刻发放
        boolean isBeginIssue = dto.getIssueBeginTime() == null || !dto.getIssueBeginTime().isAfter(now);
        Coupon tmp = BeanUtils.copyBean(dto, Coupon.class);
        if (isBeginIssue) {
            tmp.setStatus(CouponStatus.ISSUING);
            tmp.setIssueBeginTime(now);
        } else {
            tmp.setStatus(CouponStatus.UN_ISSUE);
        }
        this.updateById(tmp);

        //5.如果优惠券是立刻发放，将优惠券存入redis 的hash结构（优惠券id、领券的开始时间和结束时间、发行的总数量、限领数量）
        //TODO：考虑是否将库存分片
        if (isBeginIssue) {
            Long shopId = UserContext.getUser();
            String key = String.format(PromotionConstants.SHOP_COUPON_CHECK_KEY_FORMAT, shopId, id);//"prs:check:coupon:商家id:优惠券id"
//            Map<String, String> map = new HashMap<>(4);
//            map.put("issueBeginTime", String.valueOf(DateUtils.toEpochMilli(now)));
//            map.put("issueEndTime", String.valueOf(DateUtils.toEpochMilli(dto.getIssueEndTime())));
//            map.put("totalNum", String.valueOf(coupon.getTotalNum()));
//            map.put("userLimit", String.valueOf(coupon.getUserLimit()));

            CouponCacheVO cacheVO = BeanUtils.copyBean(coupon, CouponCacheVO.class);
            cacheVO.setIssueBeginTime(tmp.getIssueBeginTime());
            cacheVO.setIssueEndTime(tmp.getIssueEndTime());
            cacheVO.setTermDays(tmp.getTermDays());
            cacheVO.setTermEndTime(tmp.getTermBeginTime());
            cacheVO.setInitNum(coupon.getTotalNum());
            Map<String, String> map = cacheVO.toMap();
            distributedCacheService.addHash(key, map);

//            String strKey = PromotionConstants.SHOP_COUPON_CACHE_KEY_PREFIX + shopId;  //prs:shop:coupon:商家id
//            CouponVO vo = BeanUtils.copyBean(cacheVO, CouponVO.class);
//            String jsonStr = redisTemplate.opsForValue().get(strKey);
//            List<CouponVO> list = JsonUtils.toList(jsonStr, CouponVO.class);
//            list.add(vo);
//            redisTemplate.opsForValue().setIfPresent(strKey, JsonUtils.toJsonStr(list));
        }

        //6.如果优惠券的 领取方式为 指定发放 且 优惠券之前的状态为待发放， 需要生成兑换码
        if (coupon.getObtainWay() == ObtainType.ISSUE && coupon.getStatus() == CouponStatus.DRAFT) {
            //兑换码兑换的截止时间，就是优惠券领取的截至时间，该时间从前端传递时候封装到tmp中了
            coupon.setIssueEndTime(tmp.getIssueEndTime());
            exchangeCodeService.asycGenerateExchangeCode(coupon);  //异步生成兑换码
        }
    }

    private Map<Integer, Integer> calculateBuckets(Integer totalNum, int bucketNum) {
        HashMap<Integer, Integer> map = new HashMap<>();
        int perBucketNum = totalNum / bucketNum;
        int lastBucketNum = totalNum - perBucketNum * (bucketNum - 1);
        for (int i = 0; i < bucketNum - 1; i++) {
            map.put(i, perBucketNum);
        }
        map.put(bucketNum - 1, lastBucketNum);
        return map;
    }

//    /**
//     * 查询当前商家正在发放中的优惠券（升级版） 走缓存
//     *
//     * @param shopId 商家id（creater）
//     * @return
//     */
//    @Override
//    public List<CouponVO> queryIssuingCouponsByshopId(Long shopId) {
//        //1.校验参数
//        if (shopId == null) {
//            throw new BadRequestException("商家id不能为空");
//        }
//        //2.查询缓存 TODO 缓存穿透缓存击穿
//        String key = PromotionConstants.SHOP_COUPON_CACHE_KEY_PREFIX + shopId;  //prs:shop:coupon:商家id
//        String jsonStr = distributedCacheService.getString(key);
//        List<CouponVO> couponVOList = JsonUtils.toList(jsonStr, CouponVO.class);
//
//        if (couponVOList.isEmpty()) {
//            throw new BizIllegalException("当前商家不存在优惠券");
//        }
//        couponVOList.forEach(coupon -> {
//            //prs:coupon:商家id:优惠券id
//            String couponKey = PromotionConstants.COUPON_CACHE_KEY_PREFIX + shopId + ":" + coupon.getId();
//            String userCouponKey = String.format(PromotionConstants.SHOP_COUPON_ISSUED_KEY_FORMAT, shopId, coupon.getId());
//            String totalNum = String.valueOf(distributedCacheService.getObject(couponKey, "totalNum"));
//            String userLimit = String.valueOf(distributedCacheService.getObject(couponKey, "userLimit"));
//            Object obj = distributedCacheService.getObject(userCouponKey, UserContext.getUser().toString());
//            String received = ObjectUtils.isNull(obj) ? "0" : String.valueOf(obj);
//            coupon.setAvailable(Integer.parseInt(totalNum) > 0 && Integer.parseInt(received) < Integer.parseInt(userLimit));
//        });
//
//        return couponVOList;
//    }

    /**
     * 查询优惠券详情
     *
     * @param shopId
     * @param couponId
     * @return
     */
    @Override
    public CouponVO detail(Long shopId, Long couponId) {
        if (shopId == null || couponId == null) {
            throw new BadRequestException("参数不能为空");
        }
        //从缓存中获取数据
        CouponBusinessCache<CouponVO> couponBusinessCache = couponCacheService.getCachedCouponDetail(shopId, couponId);
        if (couponBusinessCache.isRetryLater()) {
            throw new BizIllegalException("当前访问人数太多，请稍后再试");
        }
        if (!couponBusinessCache.isExist()) {
            throw new BizIllegalException("当前优惠券不存在");
        }
        return couponBusinessCache.getData();
    }

    /**
     * 根据状态和商家id查询优惠券
     *
     * @param shopId
     * @param status
     * @return
     */
    @Override
    public List<CouponVO> getCouponList(Long shopId, Integer status) {
        if (shopId == null || status == null) {
            throw new BadRequestException("参数不能为空");
        }
        //从缓存中获取数据
        CouponBusinessCache<List<CouponVO>> cachedCouponList = couponListCacheService.getCachedCouponList(shopId, status);
        if (cachedCouponList.isRetryLater()) {
            throw new BizIllegalException("当前访问人数太多，请稍后再试");
        }
        if (!cachedCouponList.isExist()) {
            throw new BizIllegalException("该商家暂时没有优惠券活动哦");
        }
        return cachedCouponList.getData();
    }


    /**
     * 查询发放中的优惠券  直接走数据库
     *
     * @return
     */
    @Override
    public List<CouponVO> queryIssuingCoupons() {
        //1.查询db  coupon表  条件：发放中 手动领取
        List<Coupon> coupons = this.lambdaQuery().eq(Coupon::getStatus, CouponStatus.ISSUING).eq(Coupon::getObtainWay, ObtainType.PUBLIC).list();
        if (CollUtils.isEmpty(coupons)) {
            return CollUtils.emptyList();
        }

        //2.查询用户优惠券表 user_coupon 条件：userId  发放中的优惠券id
        List<Long> couponIds = coupons.stream().map(Coupon::getId).collect(Collectors.toList()); //正在发放的优惠券id集合
        //当前用户针对正在发放中的优惠券的领取记录
        List<UserCoupon> userCouponList = userCouponService.lambdaQuery().eq(UserCoupon::getUserId, UserContext.getUser()).in(UserCoupon::getCouponId, couponIds).list();
        //2.1统计当前用户针对每一个优惠券的已经领取的数量   map的key：优惠券id  value：当前登录用户针对该券的已领取数量
        Map<Long, Long> issuedMap = userCouponList.stream().collect(Collectors.groupingBy(UserCoupon::getCouponId, Collectors.counting()));
        //2.1统计当前用户针对每一个优惠券的已经领取且未使用的数量
        Map<Long, Long> unuseMap = userCouponList.stream().filter(userCoupon -> userCoupon.getStatus() == UserCouponStatus.UNUSED).collect(Collectors.groupingBy(UserCoupon::getCouponId, Collectors.counting()));

        //3.po转vo
        List<CouponVO> voList = coupons.stream().map(coupon -> {
            CouponVO vo = BeanUtils.copyBean(coupon, CouponVO.class);
            //可领取有两个条件：1.优惠券还有剩余：issue_num < total_num  2.（统计用户优惠券表user_coupon取出当前用户已领取数量）用户已领取数量未超过限领取数 user_limit
            Long issueNum = issuedMap.getOrDefault(coupon.getId(), 0L);
            boolean available = coupon.getIssueNum() < coupon.getTotalNum() && issueNum.intValue() < coupon.getUserLimit();
            vo.setAvailable(available);
            //统计用户优惠券表user_coupon取出当前用户已领取且未使用的券的数量
            boolean received = unuseMap.getOrDefault(coupon.getId(), 0L) > 0;
            vo.setReceived(received);  //是否可以使用
            return vo;
        }).collect(Collectors.toList());

        return voList;
    }

}
