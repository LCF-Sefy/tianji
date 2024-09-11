package com.tianji.learning.dubbo.server;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.tianji.api.interfaces.learning.LearningDubboService;
import com.tianji.learning.service.ILearningLessonService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;

@DubboService(version = "1.0.0", interfaceClass = LearningDubboService.class)
public class LearningDubboServiceImpl implements LearningDubboService {

    @Autowired
    private ILearningLessonService learningLessonService;

    @Override
    @SentinelResource(value = "QUEUE-DATA-DEGRADE", fallback = "degradeMethod")
    public void sayHello(String name) {
//        int i = 1 / 0;
        System.out.println("Hello!" + name);
    }

    public void degradeMethod(String name, Throwable t) {
        System.out.println("Degrade method called with name: " + name + ", exception: " + t.getMessage());
    }

    @Override
    public void testSeate() {
        learningLessonService.addUserLesson(129L, Arrays.asList(1549025085494521857L));
    }
}