package com.tianji.learning.service.impl;

import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.api.dto.leanring.LearningRecordDTO;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.exceptions.DbException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.LearningRecordFormDTO;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.po.LearningRecord;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.enums.SectionType;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.service.ILearningLessonService;
import com.tianji.learning.service.ILearningRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.learning.utils.LearningRecordDelayTaskHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 学习记录表 服务实现类
 * </p>
 *
 * @author sefy
 * @since 2024-04-14
 */
@Service
@RequiredArgsConstructor
public class LearningRecordServiceImpl extends ServiceImpl<LearningRecordMapper, LearningRecord> implements ILearningRecordService {

    private final ILearningLessonService lessonService;
    private final CourseClient courseClient;
    private final LearningRecordDelayTaskHandler taskHandler;

    //查询当前用户指定课程的学习进度
    @Override
    public LearningLessonDTO queryLearningRecordByCourse(Long courseId) {
        //1.获取当前登录用户
        Long userId = UserContext.getUser();

        //2.查询课表信息 条件user_id 和 courseId
        LearningLesson lesson = lessonService.lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, courseId)
                .one();
        if (lesson == null) {
            throw new BizIllegalException("该课程未加入课表");
        }

        //3.查询学习记录 条件lesson_id 和 user_id
        List<LearningRecord> recordList = this.lambdaQuery()
                .eq(LearningRecord::getLessonId, lesson.getId())
                .eq(LearningRecord::getUserId, userId)
                .list();

        //4.封装结果返回
        LearningLessonDTO dto = new LearningLessonDTO();
        dto.setId(lesson.getCourseId());  //课表id
        dto.setLatestSectionId(lesson.getLatestSectionId());  //最近学习的小节id
        //将当前用户这门课每个小节的学习记录列表转为vo列表
        List<LearningRecordDTO> dtoList = BeanUtils.copyList(recordList, LearningRecordDTO.class);
        dto.setRecords(dtoList); //当前用户这门课每个小节的学习记录
        return dto;
    }

    //提交学习记录
    @Override
    public void addLearningRecord(LearningRecordFormDTO dto) {
        //1.获取当前登录用户
        Long userId = UserContext.getUser();

        //2.处理学习记录
        boolean isFinished = false;  //代表本小节是否第一次已经学完
        if (dto.getSectionType().equals(SectionType.VIDEO)) {
            //2.1提交的是视频播放记录
            isFinished = handleVideoRecord(userId, dto);
        } else {
            //2.2提交的是考试
            isFinished = handleExamRecord(userId, dto);
        }
        if (!isFinished) {  //如果本小节不是第一次学完，不用处理课表数据
            return;
        }
        //3.处理课表 learning_lesson
        handleLessonDate(dto);
    }

    //处理课表相关数据  只有是第一次学完才处理课表数据
    private void handleLessonDate(LearningRecordFormDTO dto) {
        //1.查询课表 learning_lesson  条件：lesson_id
        LearningLesson lesson = lessonService.getById(dto.getLessonId());
        if (lesson == null) {
            throw new BizIllegalException("课表不存在");
        }

        //2.判断是否是第一次学完 isFinished == true 表示第一次学完
        boolean allFinished = false;       //allFinished：表示该课程所有的小节是否已经学完

        //3.远程调用课程服务course查询获得该课程信息 ： 小节总数
        CourseFullInfoDTO cinfo = courseClient.getCourseInfoById(lesson.getCourseId(), false, false);
        if (cinfo == null) {
            throw new BizIllegalException("课程不存在");
        }
        Integer sectionNum = cinfo.getSectionNum();  //该课程下小节的总数

        //4.如果是第一次学完，在判断该用户对该课程下全部小节是否学完
        Integer learnedSections = lesson.getLearnedSections();  //当前用户该课程已学习的小节数（不包括当前学习记录）
        allFinished = learnedSections + 1 >= sectionNum;


        //5.更新课表learning_lesson数据
        lessonService.lambdaUpdate()
                //如果课程状态一开始为未学习状态，需要将其改为学习中。
                //.set(lesson.getStatus() == LessonStatus.NOT_BEGIN, LearningLesson::getStatus, LessonStatus.LEARNING)
                .set(lesson.getLearnedSections() == 0, LearningLesson::getStatus, LessonStatus.LEARNING)
                //所有小节学完，将课程状态改为已学完
                .set(allFinished, LearningLesson::getStatus, LessonStatus.FINISHED)
                .set(LearningLesson::getLatestSectionId, dto.getSectionId())
                .set(LearningLesson::getLatestLearnTime, dto.getCommitTime())
                //第一次学完才将已学习的小节数+1
                .setSql("learned_sections  = learned_sections + 1")
                .eq(LearningLesson::getId, dto.getLessonId())
                .update();
    }

    //处理视频播放记录  前端每隔15秒就会提交正在播放的视频的学习记录
    private boolean handleVideoRecord(Long userId, LearningRecordFormDTO dto) {
        //1.查询旧的学习记录 learning_record 条件 section_id lesson_id user_id
        LearningRecord learningRecord = queryOldRecord(dto.getLessonId(), dto.getSectionId());
/*        LearningRecord learningRecord = this.lambdaQuery()
                .eq(LearningRecord::getLessonId, dto.getLessonId())
                .eq(LearningRecord::getSectionId, dto.getSectionId())
                .eq(LearningRecord::getUserId, userId).
                one();*/

        //2.如果旧的学习记录不存在（也就是既不在redis中，也不在数据库中），需要新增学习记录
        if (learningRecord == null) {
            //3.如果不存在则新增学习记录
            LearningRecord record = BeanUtils.copyBean(dto, LearningRecord.class);
            record.setUserId(userId);
            //第一次新增学习记录，肯定未完成，默认就是false，所以不用再设置
            boolean result = this.save(record);
            if (!result) {
                throw new DbException("新增考试记录失败");
            }
            return false;
        }

        //4.如果旧的学习记录存在则更新学习记录 更新的字段：moment 如果由未完成变成已完成（第一次学完） finished、finished_time
        //先判断本小节是不是第一次学完（原来的记录的finished字段为false且当前小节的播放时长>=小节的总时长的一半，说明是第一次学完）
        //不是第一次学完：1.可能是第n次学完，则前面查到的记录的finished字段肯定是true 2.可能是还未学完，查到的记录的finished = false
        boolean isFinished = !learningRecord.getFinished() && dto.getMoment() * 2 >= dto.getDuration();
        //如果不是第一次完成，需要保存（更新）学习记录到redis
        if (!isFinished) {
            LearningRecord record = new LearningRecord();
            record.setLessonId(dto.getLessonId());
            record.setSectionId(dto.getSectionId());
            record.setMoment(dto.getMoment());
            record.setFinished(learningRecord.getFinished());
            record.setId(learningRecord.getId());
            taskHandler.addLearningRecordTask(record); //这里包括缓存到redis和提交延迟检测任务
            return false;
        }

        //走这里，说明当前是第一次学完该小节
        //update learning_record set moment = xxx,finished = xxx,finished_time = xxx where id = xxx
        boolean result = this.lambdaUpdate().set(LearningRecord::getMoment, dto.getMoment())
                .set(LearningRecord::getFinished, isFinished)
                .set(isFinished, LearningRecord::getFinished, true)
                .set(isFinished, LearningRecord::getFinishTime, dto.getCommitTime())
                .eq(LearningRecord::getId, learningRecord.getId())
                .update();
        if (!result) {
            throw new DbException("更新视频学习记录失败");
        }

        //第一次学完，需要清理redis中该课表中该小节的缓存
        taskHandler.cleanRecordCache(dto.getLessonId(), dto.getSectionId());
        //5.返回是否完成 （走到这里一定是第一次完成）
        return true;
    }

    private LearningRecord queryOldRecord(Long lessonId, Long sectionId) {
        //1.查询缓存
        LearningRecord cache = taskHandler.readRecordCache(lessonId, sectionId);
        //2.如果命中，直接返回
        if (cache != null) {
            return cache;
        }
        //3.如果未命中，查询数据库并将结果放入缓存
        LearningRecord dbRecord = this.lambdaQuery()
                .eq(LearningRecord::getLessonId, lessonId)
                .eq(LearningRecord::getSectionId, sectionId)
                .one();
        //如果数据库中也没有，直接返回null，则需要后续新增学习记录
        if (dbRecord == null) {
            return null;
        }
        //4.将数据库中查询到的记录加入缓存
        taskHandler.writeRecordCache(dbRecord);
        return dbRecord;
    }

    //处理考试的学习记录
    private boolean handleExamRecord(Long userId, LearningRecordFormDTO dto) {
        //1.将传入的dto转为po
        LearningRecord record = BeanUtils.copyBean(dto, LearningRecord.class);
        record.setUserId(userId);
        record.setFinished(true);  //考试只要一提交就是完成
        record.setFinishTime(dto.getCommitTime());  //完成时间就是提交时间

        //2.保存学习记录 learning_record
        boolean result = this.save(record);
        if (!result) {
            throw new DbException("新增考试记录失败");
        }

        //3.返回学习完成状态
        //考试只要一提交就是完成，所以返回true
        return true;
    }
}

