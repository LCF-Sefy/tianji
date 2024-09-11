package com.tianji.remark.domain.msg;

import com.tianji.api.dto.msg.LikedTimesDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 点赞记录消息实体
 */
@Data
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class LikedRecordMessage {

    /**
     * 点赞业务
     */
    private String biz;

    private List<LikedTimesDTO> likedTimesDTOS;

    public List<LikedTimesDTO> getLikedTimesDTOS() {
        return likedTimesDTOS;
    }

    public void setLikedTimesDTOS(List<LikedTimesDTO> likedTimesDTOS) {
        this.likedTimesDTOS = likedTimesDTOS;
    }

    public String getBiz() {
        return biz;
    }

    public void setBiz(String biz) {
        this.biz = biz;
    }
}
