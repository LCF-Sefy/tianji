package com.tianji.api.dto.msg;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统计某业务id下 总的点赞数量 用于同步给业务方
 */
public class LikedTimesDTO {
    /**
     * 点赞的业务id
     */
    private Long bizId;
    /**
     * 总的点赞次数
     */
    private Integer likedTimes;

    public LikedTimesDTO() {
    }

    public LikedTimesDTO(Long bizId, Integer likedTimes) {
        this.bizId = bizId;
        this.likedTimes = likedTimes;
    }

    public Long getBizId() {
        return bizId;
    }

    public void setBizId(Long bizId) {
        this.bizId = bizId;
    }

    public Integer getLikedTimes() {
        return likedTimes;
    }

    public void setLikedTimes(Integer likedTimes) {
        this.likedTimes = likedTimes;
    }
}
