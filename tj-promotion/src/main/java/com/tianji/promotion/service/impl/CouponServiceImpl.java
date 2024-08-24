package com.tianji.promotion.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.*;
import com.tianji.promotion.constants.PromotionConstants;
import com.tianji.promotion.domain.dto.CouponFormDTO;
import com.tianji.promotion.domain.dto.CouponIssueFormDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.CouponScope;
import com.tianji.promotion.domain.po.UserCoupon;
import com.tianji.promotion.domain.query.CouponQuery;
import com.tianji.promotion.domain.vo.CouponPageVO;
import com.tianji.promotion.domain.vo.CouponVO;
import com.tianji.promotion.enums.CouponStatus;
import com.tianji.promotion.enums.ObtainType;
import com.tianji.promotion.enums.UserCouponStatus;
import com.tianji.promotion.mapper.CouponMapper;
import com.tianji.promotion.service.ICouponScopeService;
import com.tianji.promotion.service.ICouponService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.promotion.service.IExchangeCodeService;
import com.tianji.promotion.service.IUserCouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    private final StringRedisTemplate redisTemplate;

    /**
     * 新增优惠券
     *
     * @param dto
     */
    @Override
    @Transactional
    public void saveCoupon(CouponFormDTO dto) {
        //1.dto转po保存优惠券 coupon表
        Coupon coupon = BeanUtils.copyBean(dto, Coupon.class);
        this.save(coupon);

        //2.判断是否限定了范围 specific == false 说明无限定范围直接返回
        if (dto.getSpecific() == false) {
            return;
        }

        //3.specific == true  需要校验scopes是否为空
        List<Long> scopes = dto.getScopes();
        if (CollUtils.isEmpty(scopes)) {
            throw new BizIllegalException("分类id不能为空");
        }

        //4.保存优惠券的限定范围  coupon_scope
        List<CouponScope> csList = scopes
                .stream()
                .map(aLong -> new CouponScope().setCouponId(coupon.getId()).setType(1).setBizId(aLong))
                .collect(Collectors.toList());
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
        //1.分页条件查询查询优惠券表 Coupon
        Page<Coupon> page = this.lambdaQuery().eq(query.getType() != null, Coupon::getDiscountType, query.getType())
                .eq(query.getStatus() != null, Coupon::getStatus, query.getStatus())
                .like(StringUtils.isNotBlank(query.getName()), Coupon::getName, query.getName())
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
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
    @Transactional
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
        if (isBeginIssue) {
            String key = PromotionConstants.COUPON_CACHE_KEY_PREFIX + id;  //prs:coupon:优惠券id
            Map<String, String> map = new HashMap<>(4);
            map.put("issueBeginTime", String.valueOf(DateUtils.toEpochMilli(now)));
            map.put("issueEndTime", String.valueOf(DateUtils.toEpochMilli(dto.getIssueEndTime())));
            map.put("totalNum", String.valueOf(coupon.getTotalNum()));
            map.put("userLimit", String.valueOf(coupon.getUserLimit()));
            redisTemplate.opsForHash().putAll(key, map);
        }


        //6.如果优惠券的 领取方式为 指定发放 且 优惠券之前的状态为待发放， 需要生成兑换码
        if (coupon.getObtainWay() == ObtainType.ISSUE && coupon.getStatus() == CouponStatus.DRAFT) {
            //兑换码兑换的截止时间，就是优惠券领取的截至时间，该时间从前端传递时候封装到tmp中了
            coupon.setIssueEndTime(tmp.getIssueEndTime());
            exchangeCodeService.asycGenerateExchangeCode(coupon);  //异步生成兑换码
        }
    }

    /**
     * 查询发放中的优惠券
     *
     * @return
     */
    @Override
    public List<CouponVO> queryIssuingCoupons() {
        //1.查询db  coupon表  条件：发放中 手动领取
        List<Coupon> coupons = this.lambdaQuery()
                .eq(Coupon::getStatus, CouponStatus.ISSUING)
                .eq(Coupon::getObtainWay, ObtainType.PUBLIC)
                .list();
        if (CollUtils.isEmpty(coupons)) {
            return CollUtils.emptyList();
        }

        //2.查询用户优惠券表 user_coupon 条件：userId  发放中的优惠券id
        List<Long> couponIds = coupons.stream().map(Coupon::getId).collect(Collectors.toList()); //正在发放的优惠券id集合
        //当前用户针对正在发放中的优惠券的领取记录
        List<UserCoupon> userCouponList = userCouponService.lambdaQuery()
                .eq(UserCoupon::getUserId, UserContext.getUser())
                .in(UserCoupon::getCouponId, couponIds)
                .list();
        //2.1统计当前用户针对每一个优惠券的已经领取的数量   map的key：优惠券id  value：当前登录用户针对该券的已领取数量
        Map<Long, Long> issuedMap = userCouponList.stream()
                .collect(Collectors.groupingBy(UserCoupon::getCouponId, Collectors.counting()));
        //2.1统计当前用户针对每一个优惠券的已经领取且未使用的数量
        Map<Long, Long> unuseMap = userCouponList.stream()
                .filter(userCoupon -> userCoupon.getStatus() == UserCouponStatus.UNUSED)
                .collect(Collectors.groupingBy(UserCoupon::getCouponId, Collectors.counting()));

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
