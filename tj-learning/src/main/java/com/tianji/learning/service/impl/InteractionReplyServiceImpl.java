package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.remark.RemarkClient;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.domain.query.ReplyPageQuery;
import com.tianji.learning.domain.vo.ReplyVO;
import com.tianji.learning.enums.QuestionStatus;
import com.tianji.learning.mapper.InteractionQuestionMapper;
import com.tianji.learning.mapper.InteractionReplyMapper;
import com.tianji.learning.service.IInteractionReplyService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.tianji.common.constants.Constant.DATA_FIELD_NAME_CREATE_TIME;
import static com.tianji.common.constants.Constant.DATA_FIELD_NAME_LIKED_TIME;

/**
 * <p>
 * 互动问题的回答或评论 服务实现类
 * </p>
 *
 * @author sefy
 * @since 2024-04-19
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InteractionReplyServiceImpl extends ServiceImpl<InteractionReplyMapper, InteractionReply> implements IInteractionReplyService {

    private final InteractionQuestionMapper questionMapper;
    private final UserClient userClient;
    private final RemarkClient remarkClient;

    @Override
    public void saveReply(ReplyDTO dto) {
        //1.获取当前登录用户
        Long userId = UserContext.getUser();

        //2.保存回答或者评论 interaction_reply
        InteractionReply reply = BeanUtils.copyBean(dto, InteractionReply.class);
        reply.setUserId(userId);
        this.save(reply);

        //3.判断是否是回答 dto.answerId 为空则是回答
        //获取对应的问题数据
        InteractionQuestion question = questionMapper.selectById(dto.getQuestionId());
        if (dto.getAnswerId() != null) {
            //3.1如果不是回答   说明回复的是某个评论 累加回答下的评论次数
            this.lambdaUpdate()
                    .setSql("reply_times = reply_times + 1")
                    .eq(InteractionReply::getId, dto.getAnswerId())
                    .update();
        } else {
            //3.2如果是回答 修改问题表最近一次回答id 同时累加问题表回答次数
            question.setLatestAnswerId(reply.getId());
            question.setAnswerTimes(question.getAnswerTimes() + 1);
        }

        //4.判断是否是学生提交 dto.isStudent为true则代表学生提交 如果是则将问题表中该问题的status字段改成未查看
        if (dto.getIsStudent()) {
            //dto.isStudent为true则代表学生提交;
            question.setStatus(QuestionStatus.UN_CHECK);
        }
        questionMapper.updateById(question);
    }

    @Override
    public PageDTO<ReplyVO> queryReplyVoPage(ReplyPageQuery query) {
        //1.校验questionId和answerId是否都为空
        if (query.getQuestionId() == null && query.getAnswerId() == null) {
            throw new BadRequestException("问题id和回答不能都为空");
        }

        //2.分页查询interaction_reply表
        Page<InteractionReply> page = this.lambdaQuery()
                .eq(query.getQuestionId() != null, InteractionReply::getQuestionId, query.getQuestionId())
                .eq(InteractionReply::getAnswerId, query.getAnswerId() == null ? 0L : query.getAnswerId())
                .eq(InteractionReply::getHidden, false)
                .page(query.toMpPage( // 先根据点赞数排序，点赞数相同，再按照创建时间排序
                        new OrderItem(DATA_FIELD_NAME_LIKED_TIME, false),
                        new OrderItem(DATA_FIELD_NAME_CREATE_TIME, true))
                );
        List<InteractionReply> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(page);
        }

        //3.补全其他数据
        Set<Long> uids = new HashSet<>();
        Set<Long> targetReplyIds = new HashSet<>();
        List<Long> answerIds = new ArrayList<>();
        for (InteractionReply record : records) {
            if (!record.getAnonymity()) {
                //非匿名
                uids.add(record.getUserId());
                uids.add(record.getTargetUserId());
            }
            if (record.getTargetReplyId() != null && record.getTargetReplyId() > 0) {
                targetReplyIds.add(record.getTargetReplyId());
            }
            answerIds.add(record.getId());
        }
        // 3.2.查询目标回复，如果目标回复不是匿名，则需要查询出目标回复的用户信息
        if (targetReplyIds.size() > 0) {
            List<InteractionReply> targetReplies = listByIds(targetReplyIds);
            Set<Long> targetUserIds = targetReplies.stream()
                    .filter(Predicate.not(InteractionReply::getAnonymity))
                    .map(InteractionReply::getUserId)
                    .collect(Collectors.toSet());
            uids.addAll(targetUserIds);
        }
        //3.3.查询用户
        List<UserDTO> userDTOList = userClient.queryUserByIds(uids);
        Map<Long, UserDTO> userDTOMap = new HashMap<>();
        if (userDTOList != null) {
            userDTOMap = userDTOList.stream().collect(Collectors.toMap(UserDTO::getId, c -> c));
        }
        //3.4 查询用户点赞状态
        Set<Long> bizIds = remarkClient.getLikesStatusByBizIds(answerIds);

        //4.封装vo返回
        List<ReplyVO> voList = new ArrayList<>(records.size());
        for (InteractionReply record : records) {
            // 4.1.拷贝基础属性
            ReplyVO vo = BeanUtils.toBean(record, ReplyVO.class);
            // 4.2.回复人信息
            if (!record.getAnonymity()) {
                UserDTO userDTO = userDTOMap.get(record.getUserId());
                if (userDTO != null) {
                    vo.setUserIcon(userDTO.getIcon());
                    vo.setUserName(userDTO.getName());
                    vo.setUserType(userDTO.getType());
                }
            }
            // 4.3.如果存在评论的目标，则需要设置目标用户信息
            UserDTO targetUserDTO = userDTOMap.get(record.getTargetReplyId());
            if (targetUserDTO != null) {
                vo.setTargetUserName(targetUserDTO.getName());
            }
            //4.4 设置点赞状态
            vo.setLiked(bizIds.contains(record.getId()));
            voList.add(vo);
        }
        return PageDTO.of(page, voList);
    }
}
