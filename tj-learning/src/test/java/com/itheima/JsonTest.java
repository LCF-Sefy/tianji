package com.itheima;

import com.tianji.common.utils.JsonUtils;
import com.tianji.learning.domain.po.LearningRecord;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class JsonTest {

    @Test
    public void test() {
        List<LearningRecord> list = new ArrayList<>();
        LearningRecord record1 = new LearningRecord();
        record1.setId(1L);
        record1.setUserId(1L);
        LearningRecord record2 = new LearningRecord();
        record2.setId(2L);
        record2.setUserId(2L);
        list.add(record1);
        list.add(record2);
        String str = JsonUtils.toJsonStr(list);
        System.out.println("str = " + str);
        List<LearningRecord> ans = JsonUtils.toList(str, LearningRecord.class);
        System.out.println("ans = " + ans);

    }
}
