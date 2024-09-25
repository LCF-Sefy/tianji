package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.cache.CategoryCache;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.client.search.SearchClient;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.domain.query.QuestionAdminPageQuery;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionAdminVO;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.mapper.InteractionQuestionMapper;
import com.tianji.learning.service.IInteractionQuestionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.learning.service.IInteractionReplyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 互动提问的问题表 服务实现类
 * </p>
 *
 * @author sefy
 * @since 2024-04-19
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InteractionQuestionServiceImpl extends ServiceImpl<InteractionQuestionMapper, InteractionQuestion> implements IInteractionQuestionService {

    private final IInteractionReplyService replyService;
    private final UserClient userClient;
    private final SearchClient searchClient;
    private final CourseClient courseClient;
    private final CatalogueClient catalogueClient;
    private final CategoryCache categoryCache;

    @Override
    public void saveQuestion(QuestionFormDTO dto) {
        //1.获取当前登录用户
        Long userId = UserContext.getUser();

        //2.dto转po
        InteractionQuestion question = BeanUtils.copyBean(dto, InteractionQuestion.class);
        question.setUserId(userId);

        //3.保存
        this.save(question);
    }

    @Override
    public void updateQuestion(Long id, QuestionFormDTO dto) {
        if (StringUtils.isBlank(dto.getTitle()) || StringUtils.isBlank(dto.getDescription()) || dto.getAnonymity() == null) {
            throw new BadRequestException("非法参数");
        }
        if (dto.getTitle().length() < 1 || dto.getTitle().length() > 254) {
            throw new BadRequestException("标题长度太长");
        }
        //校验id
        InteractionQuestion question = this.getById(id);
        if (question == null) {
            throw new BadRequestException("非法参数");
        }

        //只能修改自己的互动问题
        Long userId = UserContext.getUser();
        if (userId.equals(question.getUserId())) {
            throw new BadRequestException("不能修改别人的互动问题");
        }

        //2.dto转po
        question.setTitle(dto.getTitle());
        question.setDescription(dto.getDescription());
        question.setAnonymity(dto.getAnonymity());

        //3.修改
        this.updateById(question);
    }

    @Override
    public PageDTO<QuestionVO> queryQuestionPage(QuestionPageQuery query) {
        //1.校验 参数 courseId
        if (query.getCourseId() == null) {
            throw new BadRequestException("课程id不能为空");
        }

        //2.获取当前登录用户
        Long userId = UserContext.getUser();

        //3.分页查询互动问题interaction_question 条件：courseId onlyMine为true才会加userId 小节id不能为空 hidden=false 按提问时间倒序
        Page<InteractionQuestion> page = this.lambdaQuery()
                //指定不查询description这个字段（注意取反）
                .select(InteractionQuestion.class, tableFieldInfo -> !tableFieldInfo.getProperty().equals("description"))
                .eq(InteractionQuestion::getCourseId, query.getCourseId())
                .eq(query.getOnlyMine(), InteractionQuestion::getUserId, userId)
                .eq(query.getSectionId() != null, InteractionQuestion::getSectionId, query.getSectionId())
                .eq(InteractionQuestion::getHidden, false)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<InteractionQuestion> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(page);
        }
        //将每个互动问题的最新回答replyId组成集合
        Set<Long> latestAnswerIds = new HashSet<>(); //互动问题的 最新回答id集合
        Set<Long> userIds = new HashSet<>(); //互动问题的 用户id集合（包括发布问题的用户id，和最新回答的用户id）
        for (InteractionQuestion record : records) {   //将非匿名发布问题的用户id加入set
            if (!record.getAnonymity()) {
                userIds.add(record.getUserId());
            }
            if (record.getLatestAnswerId() != null) {
                latestAnswerIds.add(record.getLatestAnswerId());
            }
        }
/*        Set<Long> latestAnswerIds = records.stream()
                .filter(c -> c.getLatestAnswerId() != null)
                .map(InteractionQuestion::getLatestAnswerId)
                .collect(Collectors.toSet());*/

        //4.根据最新回答ids 批量查询回答信息  interaction_reply  条件 in latestAnswerIds集合
        Map<Long, InteractionReply> replyMap = new HashMap<>();
        if (CollUtils.isNotEmpty(latestAnswerIds)) {  //集合可能为空，需要做非空 校验
//            List<InteractionReply> replyList = replyService.listByIds(latestAnswerIds);
            List<InteractionReply> replyList = replyService.list(Wrappers.<InteractionReply>lambdaQuery()
                    .in(InteractionReply::getId, latestAnswerIds)
                    .eq(InteractionReply::getHidden, false));
            for (InteractionReply reply : replyList) {
                if (!reply.getAnonymity()) {         //将非匿名的最新回答的用户id 存入userIds
                    userIds.add(reply.getUserId());
                }
                replyMap.put(reply.getId(), reply);
            }
            // replyMap = replyList.stream().collect(Collectors.toMap(InteractionReply::getId, c -> c));
        }

        //5.远程调用用户服务user 获取用户信息 批量
        List<UserDTO> userDTOS = userClient.queryUserByIds(userIds);
        Map<Long, UserDTO> userDTOMap = userDTOS.stream().collect(Collectors.toMap(UserDTO::getId, c -> c));

        //6.封装vo返回
        List<QuestionVO> voList = new ArrayList<>();
        for (InteractionQuestion record : records) {
            QuestionVO vo = BeanUtils.copyBean(record, QuestionVO.class);
            if (!vo.getAnonymity()) {    //问题发布的用户是非匿名的，要设置用户名称和头像
                UserDTO userDTO = userDTOMap.get(record.getUserId());
                if (userDTO != null) {
                    vo.setUserName(userDTO.getName());
                    vo.setUserIcon(userDTO.getIcon());
                }
            }
            InteractionReply reply = replyMap.get(record.getLatestAnswerId());
            if (reply != null) {
                if (!reply.getAnonymity()) {   //最新回答如果是非匿名彩色或者最新回答者的昵称
                    UserDTO userDTO = userDTOMap.get(reply.getUserId());
                    if (userDTO != null) {
                        vo.setLatestReplyUser(userDTO.getName());   //最新回答者的昵称
                    }
                }
                vo.setLatestReplyContent(reply.getContent()); //最近回答信息
            }
            voList.add(vo);
        }
        return PageDTO.of(page, voList);
    }

    @Override
    public QuestionVO queryQuestionById(Long id) {
        //1.校验
        if (id == null) {
            throw new BadRequestException("非法参数");
        }

        //2.查询互动问题表 按主键查询
        InteractionQuestion question = this.getById(id);
        if (question == null) {
            throw new BadRequestException("问题不存在");
        }

        //3.如果该问题管理员设置了因此  返回null
        if (question.getHidden()) {
            return null;
        }

        //4.封装vo
        QuestionVO questionVO = BeanUtils.copyBean(question, QuestionVO.class);

        //5.如果提问用户是匿名的，不用查询提问者昵称和头像
        if (!question.getAnonymity()) {
            //调用用户服务
            UserDTO userDTO = userClient.queryUserById(question.getUserId());
            if (userDTO != null) {
                questionVO.setUserName(userDTO.getName());
                questionVO.setUserIcon(userDTO.getIcon());
            }
        }
        return questionVO;
    }

    @Override
    public PageDTO<QuestionAdminVO> queryQuestionAdminVOPage(QuestionAdminPageQuery query) {
        //0.如果用户传了课程的名称参数，则从elasticSearch中获取该名称对应的课程id
        String courseName = query.getCourseName();
        List<Long> courseIds = new ArrayList<>();  //包含要搜索关键字的课程对应课程id集合
        if (StringUtils.isNotBlank(courseName)) {
            //通过feign远程调用搜索服务，从es中搜索该关键字对应的课程id
            courseIds = searchClient.queryCoursesIdByName(query.getCourseName());
            if (CollUtils.isEmpty(courseIds)) {
                return PageDTO.empty(0L, 0L);
            }
        }

        //1.查询互动问题表  条件前端传条件了就添加条件  分页 排序：提问时间倒序
        Page<InteractionQuestion> page = this.lambdaQuery()
                .in(CollUtils.isNotEmpty(courseIds), InteractionQuestion::getCourseId, courseIds)
                .eq(query.getStatus() != null, InteractionQuestion::getStatus, query.getStatus())
                .between(query.getBeginTime() != null && query.getEndTime() != null,
                        InteractionQuestion::getCreateTime, query.getBeginTime(), query.getEndTime())
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<InteractionQuestion> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(0L, 0L);
        }

        Set<Long> uids = new HashSet<>();  //用户id集合
        Set<Long> cids = new HashSet<>();   //课程id集合
        Set<Long> chapterAndSectionsIds = new HashSet<>();
        for (InteractionQuestion record : records) {
            uids.add(record.getUserId());
            courseIds.add(record.getCourseId());
            chapterAndSectionsIds.add(record.getChapterId());   //章id
            chapterAndSectionsIds.add(record.getSectionId());   //节id
        }

        //2.远程调用用户服务 获取用户信息
        List<UserDTO> userDTOS = userClient.queryUserByIds(uids);
        if (CollUtils.isEmpty(userDTOS)) {
            throw new BadRequestException("用户不存在");
        }
        Map<Long, UserDTO> userDTOMap = userDTOS.stream().collect(Collectors.toMap(UserDTO::getId, c -> c));

        //3.远程调用课程服务 获取课程信息
        List<CourseSimpleInfoDTO> cinfos = courseClient.getSimpleInfoList(courseIds);
        if (CollUtils.isEmpty(cinfos)) {
            throw new BadRequestException("课程不存在");
        }
        Map<Long, CourseSimpleInfoDTO> cinfoMap = cinfos.stream().collect(Collectors.toMap(CourseSimpleInfoDTO::getId, c -> c));

        //4.远程调用课程服务 获取章节信息
        List<CataSimpleInfoDTO> cataSimpleInfoDTOS = catalogueClient.batchQueryCatalogue(chapterAndSectionsIds);
        if (CollUtils.isEmpty(cataSimpleInfoDTOS)) {
            throw new BadRequestException("章节信息不存在");
        }
        Map<Long, String> cataInfoDTO = cataSimpleInfoDTOS.stream().collect(Collectors.toMap(CataSimpleInfoDTO::getId, c -> c.getName()));

        //6.封装vo返回
        List<QuestionAdminVO> voList = new ArrayList<>();
        for (InteractionQuestion record : records) {
            QuestionAdminVO adminVO = BeanUtils.copyBean(record, QuestionAdminVO.class);
            UserDTO userDTO = userDTOMap.get(record.getUserId());
            if (userDTO != null) {
                adminVO.setUserName(userDTO.getName());
            }
            CourseSimpleInfoDTO cinfoDTO = cinfoMap.get(record.getCourseId());
            if (cinfoDTO != null) {
                adminVO.setCourseName(cinfoDTO.getName());
                List<Long> categoryIds = cinfoDTO.getCategoryIds();  //一二三级分类id集合
                //5.获取课程一二三级分类id
                String categoryNames = categoryCache.getCategoryNames(categoryIds);
                adminVO.setCategoryName(categoryNames);  //三级分类名称，拼接字段
            }
            adminVO.setChapterName(cataInfoDTO.get(record.getChapterId()));  //章名称
            adminVO.setSectionName(cataInfoDTO.get(record.getSectionId()));   //节名称

            voList.add(adminVO);
        }
        return PageDTO.of(page, voList);
    }
}
