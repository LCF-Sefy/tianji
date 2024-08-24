package com.tianji.learning.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.domain.dto.LearningPlanDTO;
import com.tianji.learning.domain.po.LearningLesson;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.domain.vo.LearningPlanPageVO;

import java.util.List;

/**
 * <p>
 * 学生课程表 服务类
 * </p>
 *
 * @author sefy
 * @since 2024-04-12
 */
public interface ILearningLessonService extends IService<LearningLesson> {

    //给用户添加课表
    void addUserLesson(Long userId, List<Long> courseIds);

    //分页查询我的课表
    PageDTO<LearningLessonVO> queryMyLessons(PageQuery query);

    //查询正在学习的课程
    LearningLessonVO queryMyCurrentLesson();

    //删除用户课程表中的课程
    void deleteUserLesson(Long userId, Long courseId);

    //校验当前用户是否可以学习该课程
    Long isLessonValid(Long courseId);

    //根据课程id查询指定课程信息
    LearningLessonVO queryLessonByCourseId(Long courseId);

    //根据课程id统计该课程学习人数
    Integer countLearningLessonByCourse(Long courseId);

    //创建学习计划
    void createLearningPlan(LearningPlanDTO dto);

    //分页查询我的课程计划
    LearningPlanPageVO queryMyPlans(PageQuery query);
}
