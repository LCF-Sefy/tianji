package com.tianji.api.dto.msg;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.units.qual.A;

import java.util.List;

/**
 * 点赞数消息实体
 */
public class LikedTimesMessage {

    /**
     * 点赞业务
     */
    private String bizType;

    List<LikedTimesDTO> likedTimesDTOList;

    public LikedTimesMessage() {
    }

    public LikedTimesMessage(String bizType, List<LikedTimesDTO> likedTimesDTOList) {
        this.bizType = bizType;
        this.likedTimesDTOList = likedTimesDTOList;
    }

    public String getBizType() {
        return bizType;
    }

    public void setBizType(String bizType) {
        this.bizType = bizType;
    }

    public List<LikedTimesDTO> getLikedTimesDTOList() {
        return likedTimesDTOList;
    }

    public void setLikedTimesDTOList(List<LikedTimesDTO> likedTimesDTOList) {
        this.likedTimesDTOList = likedTimesDTOList;
    }
}
