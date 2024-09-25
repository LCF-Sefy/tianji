package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.domain.dto.LearningPlanDTO;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.domain.vo.LearningPlanPageVO;
import com.tianji.learning.service.ILearningLessonService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 学生课程表 前端控制器
 * </p>
 *
 * @author sefy
 * @since 2024-04-12
 */
@RestController
@RequestMapping("/lessons")
@Api(tags = "我的课程相关接口")
@RequiredArgsConstructor
public class LearningLessonController {

    final ILearningLessonService lessonService;

    @GetMapping("/page")
    @ApiOperation("分页查询我的课表")
    public PageDTO<LearningLessonVO> queryMyLessons(PageQuery query) {
        return lessonService.queryMyLessons(query);
    }

    @ApiOperation("查询我正在学习的课程")
    @GetMapping("/now")
    public LearningLessonVO queryMyCurrentLesson() {
        return lessonService.queryMyCurrentLesson();
    }

    @ApiOperation("删除指定课程信息")
    @DeleteMapping("/{courseId}")
    public void deleteCourseFromLesson(@PathVariable(value = "courseId") Long courseId) {
        lessonService.deleteUserLesson(null, courseId);
    }

    @ApiOperation("校验当前用户是否可以学习该课程")
    @GetMapping("/{courseId}/valid")
    public Long isLessonValid(@PathVariable(value = "courseId") Long courseId) {
        return lessonService.isLessonValid(courseId);
    }

    @ApiOperation("根据课程id查询指定课程信息")
    @GetMapping("/{courseId}")
    public LearningLessonVO queryLessonByCourseId(@PathVariable(value = "courseId") Long courseId) {
        return lessonService.queryLessonByCourseId(courseId);
    }

    @ApiOperation("根据课程id统计课程学习人数")
    @GetMapping("{courseId}/count")
    public Integer countLearningLessonByCourse(@PathVariable(value = "courseId") Long courseId) {
        return lessonService.countLearningLessonByCourse(courseId);
    }

    @ApiOperation("创建学习计划")
    @PostMapping("plans")
    public void createLearningPlan(@RequestBody @Validated LearningPlanDTO dto) {
        lessonService.createLearningPlan(dto);
    }

    @ApiOperation("分页查询我的课程计划")
    @GetMapping("plans")
    public LearningPlanPageVO queryMyPlans(PageQuery query){
        return lessonService.queryMyPlans(query);
    }
}
