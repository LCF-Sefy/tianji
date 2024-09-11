package com.tianji.learning.domain.msg;

import com.tianji.learning.enums.PointsRecordType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

public class PointMessage implements Serializable {
    //获取积分的途径类型
    private Integer type;
    //获取积分的用户id
    private Long userId;
    //获取的积分数
    private Integer points;

    public PointMessage() {}

    public PointMessage(Integer type, Long userId, Integer points) {
        this.type = type;
        this.userId = userId;
        this.points = points;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }
}