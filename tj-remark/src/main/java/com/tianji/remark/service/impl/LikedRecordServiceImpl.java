//package com.tianji.remark.service.impl;
//
//import com.tianji.api.dto.msg.LikedTimesDTO;
//import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
//import com.tianji.common.constants.MqConstants;
//import com.tianji.common.utils.BeanUtils;
//import com.tianji.common.utils.CollUtils;
//import com.tianji.common.utils.StringUtils;
//import com.tianji.common.utils.UserContext;
//import com.tianji.remark.domain.dto.LikeRecordFormDTO;
//import com.tianji.remark.domain.po.LikedRecord;
//import com.tianji.remark.mapper.LikedRecordMapper;
//import com.tianji.remark.service.ILikedRecordService;
//import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//import java.util.Set;
//import java.util.stream.Collectors;
//
///**
// * <p>
// * 点赞记录表 服务实现类   未改进前
// * </p>
// *
// * @author sefy
// * @since 2024-05-01
// */
//@Service
//@Slf4j
//@RequiredArgsConstructor
//public class LikedRecordServiceImpl extends ServiceImpl<LikedRecordMapper, LikedRecord> implements ILikedRecordService {
//
//    private final RabbitMqHelper rabbitMqHelper;
//
//    @Override
//    public void addLikeRecord(LikeRecordFormDTO dto) {
//        //1.获取当前登录用户
//        Long userId = UserContext.getUser();
//
//        //2.判断是否点赞 dto.liked == true 则是点赞
//        boolean flag = dto.getLiked() ? liked(dto, userId) : unliked(dto, userId);
//        //点赞失败或者取消点赞失败 直接返回
//        if (!flag){
//            return;
//        }
//
//        //3.统计点赞数量
//        Integer totalLikesNum = this.lambdaQuery()
//                .eq(LikedRecord::getBizId, dto.getBizId())
//                .count();
//
//        //4.发送MQ通知
//        String routingKey = StringUtils.format(MqConstants.Key.LIKED_TIMES_KEY_TEMPLATE, dto.getBizType());
//        LikedTimesDTO msg = LikedTimesDTO.of(dto.getBizId(), totalLikesNum);
//        log.debug("发送点赞消息，消息内容：{}", msg);
//        rabbitMqHelper.send(
//                MqConstants.Exchange.LIKE_RECORD_EXCHANGE,
//                routingKey,
//                msg
//        );
//
//    }
//
//    //取消点赞逻辑
//    private boolean unliked(LikeRecordFormDTO dto, Long userId) {
//        //先判断是否已经点赞（判断是否存在点赞记录）  查询数据库
//        LikedRecord record = this.lambdaQuery()
//                .eq(LikedRecord::getUserId, userId)
//                .eq(LikedRecord::getBizId, dto.getBizId())
//                .one();
//        if (record == null) {
//            //点赞不存在 取消点赞失败
//            return false;
//        }
//        //否则删除点赞记录
//        boolean result = this.removeById(record.getId());
//        return result;
//    }
//
//    //点赞逻辑
//    private boolean liked(LikeRecordFormDTO dto, Long userId) {
//        //先判断是否已经点赞（判断是否存在点赞记录）  查询数据库
//        LikedRecord record = this.lambdaQuery()
//                .eq(LikedRecord::getUserId, userId)
//                .eq(LikedRecord::getBizId, dto.getBizId())
//                .one();
//        if (record != null) {
//            //点赞存在 点赞失败
//            return false;
//        }
//        //否则新增点赞记录
//        LikedRecord likedRecord = BeanUtils.copyBean(dto, LikedRecord.class);
//        likedRecord.setUserId(userId);
//        boolean result = this.save(likedRecord);
//        return result;
//    }
//
//    @Override
//    public Set<Long> getLikesStatusByBizIds(List<Long> bizIds) {
//        if (CollUtils.isEmpty(bizIds)) {
//            return CollUtils.emptySet();
//        }
//        //1.获取当前用户
//        Long userId = UserContext.getUser();
//
//        //2.查询点赞记录表
//        List<LikedRecord> recordList = this.lambdaQuery()
//                .eq(LikedRecord::getUserId, userId)
//                .in(LikedRecord::getBizId, bizIds)
//                .list();
//
//        //3.将查询到的bizId转成集合返回
//        Set<Long> likedBizIds = recordList.stream().map(LikedRecord::getBizId).collect(Collectors.toSet());
//
//        //4.返回
//        return likedBizIds;
//    }
//}
