package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.LearningPlanDTO;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.po.LearningRecord;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.domain.vo.LearningPlanPageVO;
import com.tianji.learning.domain.vo.LearningPlanVO;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.enums.PlanStatus;
import com.tianji.learning.mapper.LearningLessonMapper;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.service.ILearningLessonService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 学生课程表 服务实现类
 * </p>
 *
 * @author sefy
 * @since 2024-04-12
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LearningLessonServiceImpl extends ServiceImpl<LearningLessonMapper, LearningLesson> implements ILearningLessonService {

    final CourseClient courseClient;

    final CatalogueClient catalogueClient;

    final LearningRecordMapper recordMapper;

    //给用户添加课表
    @Override
    public void addUserLesson(Long userId, List<Long> courseIds) {
        //1.通过feign远程调用课程服务course，得到课程信息
        List<CourseSimpleInfoDTO> cinfos = courseClient.getSimpleInfoList(courseIds);

        //2.封装po实体类，填充课程过期时间
        List<LearningLesson> list = new ArrayList<>();
        for (CourseSimpleInfoDTO cinfo : cinfos) {
            LearningLesson lesson = new LearningLesson();
            lesson.setUserId(userId);
            lesson.setCourseId(cinfo.getId());
            Integer validDuration = cinfo.getValidDuration();  //课程的有效期 单位：月
            if (validDuration != null) {
                LocalDateTime now = LocalDateTime.now();
                lesson.setCreateTime(now);
                //设置课程过期时间
                lesson.setExpireTime(now.plusMonths(validDuration));
            }
            list.add(lesson);
        }

//        int i = 1 / 0;

        //3.批量保存
        this.saveBatch(list);
    }

    //分页查询我的课表
    @Override
    public PageDTO<LearningLessonVO> queryMyLessons(PageQuery query) {
        //1.获取当前登陆人
        Long userId = UserContext.getUser();
        if (userId == null) {
            throw new BadRequestException("必须得登录");
        }

        //2.分页查询我的课表
        //select * from learning_lesson where user_id = #{userId} order by latest_learn_time limit 0, 5
        Page<LearningLesson> page = lambdaQuery()
                .eq(LearningLesson::getUserId, userId) // where user_id = #{userId}
                .page(query.toMpPage("latest_learn_time", false));
        List<LearningLesson> records = page.getRecords();
        //没有课程返回空课程表
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(page);
        }

        //3.远程调用课程服务course 查询到的信息用于给vo课程名、课程封面、章节数量赋值
        Set<Long> courseIds = records.stream().map(LearningLesson::getCourseId).collect(Collectors.toSet());
        List<CourseSimpleInfoDTO> cinfos = courseClient.getSimpleInfoList(courseIds);
        if (CollUtils.isEmpty(cinfos)) {
            throw new BizIllegalException("课程不存在");
        }
        //将cinfos 课程集合 转换为 map结构<课程id，课程对象>
        Map<Long, CourseSimpleInfoDTO> infoDTOMap =
                cinfos.stream().collect(Collectors.toMap(CourseSimpleInfoDTO::getId, c -> c));

        //4.将po中的数据 封装到vo中
        List<LearningLessonVO> voList = new ArrayList<>();
        for (LearningLesson record : records) {
            //拷贝属性到dto
            LearningLessonVO vo = BeanUtils.copyBean(record, LearningLessonVO.class);
            //获取课程信息，填充vo课程名、课程封面、章节数量赋值
            CourseSimpleInfoDTO infoDTO = infoDTOMap.get(record.getCourseId());
            if (infoDTO != null) {
                vo.setCourseName(infoDTO.getName());
                vo.setCourseCoverUrl(infoDTO.getCoverUrl());
                vo.setSections(infoDTO.getSectionNum());
            }
            voList.add(vo);
        }

        //5.返回
        return PageDTO.of(page, voList);
    }

    //查询正在学习的课程
    @Override
    public LearningLessonVO queryMyCurrentLesson() {
        //1.获取当前登录用户id
        Long userId = UserContext.getUser();

        //2.查询当前用户最近学习课程  按latest_learn_time 降序排列 取第一条   正在学习中的 status = 1
        //select * from learning_lesson where user_id = #{userId} AND status = 1 order by latest_learn_time limit 1
        LearningLesson lesson = this.lambdaQuery().eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getStatus, LessonStatus.LEARNING)
                .orderByDesc(LearningLesson::getLatestLearnTime)
                .last("limit 1")
                .one();
        if (lesson == null) {
            return null;
        }

        //3.远程调用课程服务course 查询到的信息用于给vo课程名、课程封面、章节数量赋值
        CourseFullInfoDTO cinfo = courseClient.getCourseInfoById(lesson.getUserId(), false, false);
        if (cinfo == null) {
            throw new BizIllegalException("课程不存在");
        }

        //4.查询当前登录用户课表中 总的课程数
        //select count(*) from learning_lesson where user_id = xxx
        Integer count = this.lambdaQuery().eq(LearningLesson::getUserId, userId).count();

        //5.通过feign远程调用课程服务course  获取小节名称 和 小节编号
        Long latestSectionId = lesson.getLatestSectionId();  //最近学习的小节id
        List<CataSimpleInfoDTO> cataSimpleInfoDTOS = catalogueClient.batchQueryCatalogue(CollUtils.singletonList(latestSectionId));
        if (CollUtils.isEmpty(cataSimpleInfoDTOS)) {
            throw new BizIllegalException("小节不存在");
        }

        //6.封装vo返回
        LearningLessonVO vo = BeanUtils.copyBean(lesson, LearningLessonVO.class);
        vo.setCourseCoverUrl(cinfo.getCoverUrl());
        vo.setCourseName(cinfo.getName());
        vo.setSections(cinfo.getSectionNum());
        vo.setCourseAmount(count);   //当前用户能学习课程总数
        CataSimpleInfoDTO cataSimpleInfoDTO = cataSimpleInfoDTOS.get(0);
        vo.setLatestSectionIndex(cataSimpleInfoDTO.getCIndex());  //最近学习的小节名称
        vo.setLatestSectionName(cataSimpleInfoDTO.getName());  //最近学习的小节序号

        return vo;
    }

    //删除用户课程表中的课程
    @Override
    public void deleteUserLesson(Long userId, Long courseId) {
        //1.获取当前登录用户
        if (userId == null) {
            userId = UserContext.getUser();
        }
        //删除课程
        this.remove(buildUserIdAndCourseIdWrapper(userId, courseId));
    }

    //校验当前用户是否可以学习该课程
    @Override
    public Long isLessonValid(Long courseId) {
        //1.获取当前登录用户
        Long userId = UserContext.getUser();
        if (userId == null) {
            throw new BadRequestException("必须得登录");
        }

        //2.查询该用户课表，看是否有这门课
        LearningLesson lesson = this.getOne(buildUserIdAndCourseIdWrapper(userId, courseId));
        if (lesson == null) {
            return null;
        }
        LocalDateTime expireTime = lesson.getExpireTime();

        //3.返回结果
        if (expireTime != null && expireTime.isBefore(LocalDateTime.now())) {
            //如果过期时间在当前时间之前，代表课程已经失效，返回null
            return null;
        }
        return lesson.getId();
    }

    //根据课程id查询指定课程信息
    @Override
    public LearningLessonVO queryLessonByCourseId(Long courseId) {
        //1.获取当前用户
        Long userId = UserContext.getUser();
        if (userId == null) {
            throw new BadRequestException("必须得登录");
        }

        //2.查询课程表中的课程信息
        LearningLesson lesson = this.getOne(buildUserIdAndCourseIdWrapper(userId, courseId));
        if (lesson == null) {
            throw new BizIllegalException("课程不存在");
        }

        //3.封装返回
        LearningLessonVO vo = BeanUtils.copyBean(lesson, LearningLessonVO.class);
        return vo;
    }

    //根据课程id统计该课程学习人数
    @Override
    public Integer countLearningLessonByCourse(Long courseId) {
        //select count(*) from learning_lesson where course_id = xxx and status in (0,1,2)
        Integer count = lambdaQuery().eq(LearningLesson::getCourseId, courseId)
                .in(LearningLesson::getStatus, LessonStatus.NOT_BEGIN, LessonStatus.LEARNING, LessonStatus.FINISHED)
                .count();
        return count;
    }

    //创建学习计划
    @Override
    public void createLearningPlan(LearningPlanDTO dto) {
        //1.获取登录用户
        Long userId = UserContext.getUser();

        //2.查询课程表中的课程信息
        LearningLesson lesson = this.getOne(buildUserIdAndCourseIdWrapper(userId, dto.getCourseId()));
        if (lesson == null) {
            throw new BizIllegalException("该课程没有加入课表");
        }

        //3.修改课表
        this.lambdaUpdate()
                .set(LearningLesson::getWeekFreq, dto.getFreq())
                .set(lesson.getPlanStatus() == PlanStatus.NO_PLAN, LearningLesson::getPlanStatus, PlanStatus.PLAN_RUNNING)
                .eq(LearningLesson::getId, lesson.getId())
                .update();
    }

    //分页查询我的课程计划
    @Override
    public LearningPlanPageVO queryMyPlans(PageQuery query) {
        //1.获取当前登录用户
        Long userId = UserContext.getUser();

        //todo 2.查询积分

        //3.查询本周学习总数  learning_lesson 条件：userId, status in (0,1),plan_status = 1, sum(week_freq)
        //SELECT SUM(week_freq) FROM `learning_lesson` learning_lesson WHERE user_id = 2 AND `status` IN(0,1) AND plan_status = 1
        QueryWrapper<LearningLesson> wrapper = new QueryWrapper<>();
        wrapper.select("sum(week_freq) as plansTotal");  //查询哪些列
        wrapper.eq("user_id", userId);
        wrapper.in("status", LessonStatus.NOT_BEGIN, LessonStatus.LEARNING);
        wrapper.eq("plan_status", PlanStatus.PLAN_RUNNING);
        Map<String, Object> map = this.getMap(wrapper);
        //{plansTotal:value}
        Integer planTotal = 0;
        if (map != null && map.get("planTotal") != null) {
            planTotal = Integer.valueOf(map.get("planTotal").toString());
        }

        //4.查询本周 实际 已学习的计划总数 lesson_record 条件： userId finish_time要在本周内 finished = true  count
        //SELECT count(*) FROM learning_record WHERE user_id = 2 AND finished = 1 and finish_time BETWEEN '2023-05-22 00:00:01' AND '2023-05-28 23:59:59'、
        LocalDate now = LocalDate.now();
        LocalDateTime weekBeginTime = DateUtils.getWeekBeginTime(now);   //本周开始时间
        LocalDateTime weekEndTime = DateUtils.getWeekEndTime(now);          //本周结束时间
        Integer weekFinishedPlanNUm = recordMapper.selectCount(Wrappers.<LearningRecord>lambdaQuery()
                .eq(LearningRecord::getUserId, userId)
                .eq(LearningRecord::getFinished, true)
                .between(LearningRecord::getFinishTime, weekBeginTime, weekEndTime));

        //5.查询课表数据 learning_lessons 条件：userId, status in (0,1),plan_status = 1 分页
        Page<LearningLesson> page = this.lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getPlanStatus, PlanStatus.PLAN_RUNNING)
                .in(LearningLesson::getStatus, LessonStatus.NOT_BEGIN, LessonStatus.LEARNING)
                .page(query.toMpPage("latest_learn_time", false));
        List<LearningLesson> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            LearningPlanPageVO vo = new LearningPlanPageVO();
            vo.setPages(0L);
            vo.setTotal(0L);
            vo.setList(CollUtils.emptyList());
            return vo;
        }

        //6.远程调用课程服务course获取课程信息
        List<Long> courseIds = records.stream().map(LearningLesson::getCourseId).collect(Collectors.toList());
        List<CourseSimpleInfoDTO> cinfos = courseClient.getSimpleInfoList(courseIds);
        if (CollUtils.isEmpty(cinfos)) {
            throw new BizIllegalException("课程不存在");
        }
        //将cinfo list结构转为 <课程id， CourseSimpleInfoDTO>
        Map<Long, CourseSimpleInfoDTO> cinfosMap = cinfos.stream().collect(Collectors.toMap(CourseSimpleInfoDTO::getId, c -> c));

        //7.查询学习记录表learning_record 当前用户下 每一门课下 已学习的小节数量
        //SELECT lesson_id,count(*) FROM learning_record WHERE user_id = 2 AND finished = 1 AND finish_time BETWEEN '2023-05-22 00:00:01' AND '2023-05-28 23:59:59' GROUP BY lesson_id
        QueryWrapper<LearningRecord> rWrapper = new QueryWrapper<>();
        rWrapper.eq("user_id", userId);
        rWrapper.eq("finished", true);
        rWrapper.between("finish_time", weekBeginTime, weekEndTime);
        rWrapper.groupBy("lesson_id");
        rWrapper.select("lesson_id as lessonId", "count(*) as userId");
        List<LearningRecord> learningRecords = recordMapper.selectList(rWrapper);
        //map中的key是 lessonId value 是当前用户对该课程下已学习的小节数量
        Map<Long, Long> courseWeekFinishNumMap = learningRecords.stream().collect(Collectors.toMap(LearningRecord::getLessonId, c -> c.getUserId()));


        //8.封装vo返回
        LearningPlanPageVO vo = new LearningPlanPageVO();
        vo.setWeekTotalPlan(planTotal);
        vo.setWeekFinished(weekFinishedPlanNUm);

        List<LearningPlanVO> voList = new ArrayList<>();
        for (LearningLesson record : records) {
            LearningPlanVO planVO = BeanUtils.copyBean(record, LearningPlanVO.class);
            CourseSimpleInfoDTO infoDTO = cinfosMap.get(record.getCourseId());
            if (infoDTO != null) {
                planVO.setCourseName(infoDTO.getName());  //课程名
                planVO.setSections(infoDTO.getSectionNum());  //课程下的总小节数
            }
            //设置当前这门课本周已学习的小节数
            planVO.setWeekLearnedSections(courseWeekFinishNumMap.getOrDefault(record.getId(), 0L).intValue());
            voList.add(planVO);
        }
        vo.pageInfo(page.getTotal(), page.getPages(), voList);
        return vo;
    }

    private LambdaQueryWrapper<LearningLesson> buildUserIdAndCourseIdWrapper(Long userId, Long courseId) {
        LambdaQueryWrapper<LearningLesson> queryWrapper = new QueryWrapper<LearningLesson>()
                .lambda()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, courseId);
        return queryWrapper;
    }
}
