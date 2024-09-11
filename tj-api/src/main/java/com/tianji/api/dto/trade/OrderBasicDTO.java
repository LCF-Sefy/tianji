package com.tianji.api.dto.trade;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderBasicDTO {
    /**
     * 订单id
     */
    private Long orderId;
    /**
     * 下单用户id
     */
    private Long userId;
    /**
     * 下单的课程id集合
     */
    private List<Long> courseIds;
    /**
     * 订单完成时间
     */
    private LocalDateTime finishTime;

    public OrderBasicDTO() {
    }

    public OrderBasicDTO(Long orderId, Long userId, List<Long> courseIds, LocalDateTime finishTime) {
        this.orderId = orderId;
        this.userId = userId;
        this.courseIds = courseIds;
        this.finishTime = finishTime;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<Long> getCourseIds() {
        return courseIds;
    }

    public void setCourseIds(List<Long> courseIds) {
        this.courseIds = courseIds;
    }

    public LocalDateTime getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(LocalDateTime finishTime) {
        this.finishTime = finishTime;
    }
}
