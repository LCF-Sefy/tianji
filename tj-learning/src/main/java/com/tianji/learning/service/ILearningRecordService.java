package com.tianji.learning.service;

import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.learning.domain.dto.LearningRecordFormDTO;
import com.tianji.learning.domain.po.LearningRecord;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 学习记录表 服务类
 * </p>
 *
 * @author sefy
 * @since 2024-04-14
 */
public interface ILearningRecordService extends IService<LearningRecord> {

    //查询当前用户指定课程的学习进度
    LearningLessonDTO queryLearningRecordByCourse(Long courseId);

    //提交学习记录
    void addLearningRecord(LearningRecordFormDTO dto);
}
