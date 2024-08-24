package com.itheima;

import com.tianji.api.client.remark.RemarkClient;
import com.tianji.learning.LearningApplication;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Set;

@SpringBootTest(classes = LearningApplication.class)
public class RemarkClientFeignTest {

    @Autowired
    private RemarkClient remarkClient;

    @Test
    public void test(){
        Set<Long> byBizIds = remarkClient.getLikesStatusByBizIds(Lists.list(1782804792544542722L, 123L));
        System.out.println("byBizIds = " + byBizIds);
    }
}
