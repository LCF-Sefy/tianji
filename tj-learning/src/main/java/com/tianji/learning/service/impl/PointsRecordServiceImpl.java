package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.domain.msg.SignInMessage;
import com.tianji.learning.domain.po.PointsRecord;
import com.tianji.learning.domain.vo.PointsStatisticsVO;
import com.tianji.learning.enums.PointsRecordType;
import com.tianji.learning.mapper.PointsRecordMapper;
import com.tianji.learning.service.IPointsRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 学习积分记录，每个月底清零 服务实现类
 * </p>
 *
 * @author sefy
 * @since 2024-05-04
 */
@Service
@RequiredArgsConstructor
public class PointsRecordServiceImpl extends ServiceImpl<PointsRecordMapper, PointsRecord> implements IPointsRecordService {

    private final StringRedisTemplate redisTemplate;

    @Override
    public void addPointRecord(SignInMessage msg, PointsRecordType type) {

        //0.校验
        if (msg.getUserId() == null || msg.getPoints() == null) {
            return;
        }
        int realPoint = msg.getPoints();  //代表实际可以增加的积分
        //1.判断该积分类型是否有上限 type.maxPoints 是否大于0
        int maxPoints = type.getMaxPoints(); //积分类型上限
        if (maxPoints > 0) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime dayStartTime = DateUtils.getDayStartTime(now);
            LocalDateTime dayEndTime = DateUtils.getDayEndTime(now);

            //2.如果有上限 要查询当前用户 该积分类型 今日已得积分 points_record 条件 userid type 今天 sum(points)
            QueryWrapper<PointsRecord> wrapper = new QueryWrapper<>();
            wrapper.select("sum(points) as totalPoints");
            wrapper.eq("user_id", msg.getUserId());
            wrapper.eq("type", type);
            wrapper.between("create_time", dayStartTime, dayEndTime);
            Map<String, Object> map = this.getMap(wrapper);
            int currentPoints = 0; //当前用户 该积分类型 已得积分
            if (map != null) {
                BigDecimal totalPoints = (BigDecimal) map.get("totalPoints");
                currentPoints = totalPoints.intValue();
            }

            //3.判断已得积分是否超过上限
            if (currentPoints >= maxPoints) {
                //说明已得积分 达到上限
                return;
            }
            //计算本次实际应该增加多少分
            if (currentPoints + realPoint > maxPoints) {
                realPoint = maxPoints - currentPoints;
            }
        }

        //4，保存积分  即添加一条积分记录
        PointsRecord record = new PointsRecord();
        record.setUserId(msg.getUserId());
        record.setType(type);
        record.setPoints(realPoint);  //分值
        this.save(record);

        //5.累加并保存总积分到redis 采用zset 当前赛季的排行榜
        LocalDate now = LocalDate.now();
        String format = now.format(DateTimeFormatter.ofPattern("yyyyMM"));
        String key = RedisConstants.POINTS_BOARD_KEY_PREFIX + format;
        redisTemplate.opsForZSet().incrementScore(key, msg.getUserId().toString(), realPoint);
    }

    @Override
    public List<PointsStatisticsVO> queryMyTodayPoints() {
        //1.获取用户id
        Long userId = UserContext.getUser();

        //2.查询积分表points_record 条件：userId  今日   按type分组  type， sum
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dayStartTime = DateUtils.getDayStartTime(now);
        LocalDateTime dayEndTime = DateUtils.getDayEndTime(now);

        QueryWrapper<PointsRecord> wrapper = new QueryWrapper<>();
        wrapper.select("type", "sum(points) as points");
        wrapper.eq("user_id", userId);
        wrapper.between("create_time", dayStartTime, dayEndTime);
        wrapper.groupBy("type");
        List<PointsRecord> list = this.list(wrapper);
        if (CollUtils.isEmpty(list)) {
            return CollUtils.emptyList();
        }

        //3.封装返回
        List<PointsStatisticsVO> voList = new ArrayList<>();
        for (PointsRecord record : list) {
            PointsStatisticsVO vo = new PointsStatisticsVO();
            vo.setType(record.getType().getDesc());
            vo.setMaxPoints(record.getType().getMaxPoints());  //积分类型上限
            vo.setPoints(record.getPoints());   //积分类型的中文
            voList.add(vo);
        }
        return voList;
    }
}
