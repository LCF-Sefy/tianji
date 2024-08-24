package com.tianji.remark.task;

import com.tianji.remark.service.ILikedRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class LikedTimesCheckTask {

    private static final List<String> BIZ_TYPES = List.of("QA", "NOTE");
    private static final int MAX_BIZ_SIZE = 30; //任务每次取的biz数量  防止一次性往mq发送消息太多

    private final ILikedRecordService likedRecordService;

    //每20秒执行一次  将redis中 业务类型下某业务的点赞总数 发送消息到mq
//    @Scheduled(cron = "0/20 * * * * ?")
    @Scheduled(fixedDelay = 20000)  //每间隔20s响应一次
    public void checkLikedTimes(){
        for (String bizType : BIZ_TYPES) {
            likedRecordService.readLikedTimesAndSendMessage(bizType, MAX_BIZ_SIZE);
        }
    }
}
