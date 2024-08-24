package com.tianji.remark.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.dto.msg.LikedTimesDTO;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.remark.constants.RedisConstants;
import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.domain.po.LikedRecord;
import com.tianji.remark.mapper.LikedRecordMapper;
import com.tianji.remark.service.ILikedRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * <p>
 * 点赞记录表 服务实现类  使用Redis改进后
 * </p>
 *
 * @author sefy
 * @since 2024-05-01
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LikedRecordRedisServiceImpl extends ServiceImpl<LikedRecordMapper, LikedRecord> implements ILikedRecordService {

    private final RabbitMqHelper rabbitMqHelper;
    private final StringRedisTemplate redisTemplate;

    @Override
    public void addLikeRecord(LikeRecordFormDTO dto) {
        //1.获取当前登录用户
        Long userId = UserContext.getUser();

        //2.判断是否点赞 dto.liked == true 则是点赞
        boolean flag = dto.getLiked() ? liked(dto, userId) : unliked(dto, userId);
        //点赞失败或者取消点赞失败 直接返回
        if (!flag) {
            return;
        }

        //3.统计该业务id的总点赞数
        //拼接key
        String key = RedisConstants.LIKE_BIZ_KEY_PREFIX + dto.getBizId();
        Long totalLikesNum = redisTemplate.opsForSet().size(key);
        if (totalLikesNum == null) {
            return;
        }

        //4.向Redis中缓存点赞总数 zset key：bizType(业务类型）  member：bizId  score：点赞数量
        String bizTypeTotalLikeKey = RedisConstants.LIKES_TIMES_KEY_PREFIX + dto.getBizType();
        redisTemplate.opsForZSet().add(bizTypeTotalLikeKey, dto.getBizId().toString(), totalLikesNum);

    }

    //点赞逻辑
    private boolean liked(LikeRecordFormDTO dto, Long userId) {
        //拼接key
        String key = RedisConstants.LIKE_BIZ_KEY_PREFIX + dto.getBizId();
        //向Redis中插入点赞记录 set key：bizId value：userId
        Long result = redisTemplate.opsForSet().add(key, userId.toString());
        return result != null && result > 0;
    }

    //取消点赞逻辑
    private boolean unliked(LikeRecordFormDTO dto, Long userId) {
        //拼接key
        String key = RedisConstants.LIKE_BIZ_KEY_PREFIX + dto.getBizId();
        //从Redis中删除点赞记录 set key：bizId value：userId
        Long result = redisTemplate.opsForSet().remove(key, userId.toString());
        return result != null && result > 0;
    }

    /**
     * 根据业务id获取当前用户对该业务的点赞状态 由于这里短时间内会执行大量的redis命令（频繁查看set是否包含某个元素）所以这里使用了Redis的管道技术
     * @param bizIds
     * @return 返回当前用户点赞的业务id
     */
    @Override
    public Set<Long> getLikesStatusByBizIds(List<Long> bizIds) {
        // 1.获取登录用户id
        Long userId = UserContext.getUser();
        // 2.查询点赞状态
        List<Object> objects = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            StringRedisConnection src = (StringRedisConnection) connection;
            for (Long bizId : bizIds) {
                String key = RedisConstants.LIKE_BIZ_KEY_PREFIX + bizId;
                src.sIsMember(key, userId.toString());
            }
            return null;
        });
        // 3.返回结果
        return IntStream.range(0, objects.size()) // 创建从0到集合size的流
                .filter(i -> (boolean) objects.get(i)) // 遍历每个元素，保留结果为true的角标i
                .mapToObj(bizIds::get)// 用角标i取bizIds中的对应数据，就是点赞过的id
                .collect(Collectors.toSet());// 收集

/*        if (CollUtils.isEmpty(bizIds)) {
            return CollUtils.emptySet();
        }
        //1.获取当前用户
        Long userId = UserContext.getUser();

        //2.查询点赞记录表
        Set<Long> likedBizIds = new HashSet<>();
        for (Long bizId : bizIds) {
            String key = RedisConstants.LIKE_BIZ_KEY_PREFIX + bizId;
            //判断该业务id 点赞用户集合中是否包含当前用户
            Boolean member = redisTemplate.opsForSet().isMember(key, userId.toString());
            if (member){
                likedBizIds.add(bizId);
            }
        }

        //3.返回
        return likedBizIds;*/
    }

    /**
     * 将redis中 业务类型下某业务的点赞总数 发送消息到mq
     * @param bizType
     * @param maxBizSize
     */
    @Override
    public void readLikedTimesAndSendMessage(String bizType, int maxBizSize) {
        //1.拼接key  likes:times:type:QA   likes:times:type:NOTE
        String bizTypeTotalLikeKey = RedisConstants.LIKES_TIMES_KEY_PREFIX + bizType;

        List<LikedTimesDTO> list = new ArrayList<>();
        //2.从redis的zset结构中按分数排序取 maxBizSize的 业务点赞信息
        Set<ZSetOperations.TypedTuple<String>> typedTuples = redisTemplate.opsForZSet().popMin(bizTypeTotalLikeKey, maxBizSize);
        for (ZSetOperations.TypedTuple<String> typedTuple : typedTuples) {
            String bizId = typedTuple.getValue();
            Double likedTimes = typedTuple.getScore();
            if (StringUtils.isBlank(bizId) || likedTimes == null) {
                continue;
            }
            //3.封装LikedTimesDTO 消息数据
            LikedTimesDTO msg = LikedTimesDTO.of(Long.valueOf(bizId), likedTimes.intValue());
            list.add(msg);
        }

        //4.发送消息到mq
        if (CollUtils.isNotEmpty(list)) {
            String routingKey = StringUtils.format(MqConstants.Key.LIKED_TIMES_KEY_TEMPLATE, bizType);
            log.debug("发送点赞消息，消息内容：{}", list);
            rabbitMqHelper.send(
                    MqConstants.Exchange.LIKE_RECORD_EXCHANGE,
                    routingKey,
                    list);
        }
    }
}
