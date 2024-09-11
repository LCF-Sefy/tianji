package com.itheima;

import com.tianji.api.dto.msg.LikedTimesDTO;
import com.tianji.api.dto.msg.LikedTimesMessage;
import com.tianji.common.utils.JsonUtils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class JsonTest {

    @Test
    public void test() {
        List<LikedTimesDTO> list = new ArrayList<>();
        list.add(LikedTimesDTO.of(1782977388850360321L, 1));
        list.add(LikedTimesDTO.of(17850360321L, 222));
        LikedTimesMessage msg = new LikedTimesMessage();
        msg.setBizType("QA");
        msg.setLikedTimesDTOList(list);
        String jsonStr = JsonUtils.toJsonStr(msg);
        System.out.println("jsonStr = " + jsonStr);
        LikedTimesMessage bean = JsonUtils.toBean(jsonStr, LikedTimesMessage.class);
        System.out.println("bean = " + bean);
        String bizType = bean.getBizType();
        List<LikedTimesDTO> list1 = bean.getLikedTimesDTOList();
        System.out.println("list1 = " + list1);
    }
}
