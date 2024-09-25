package com.tianji.stock.domain.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 优惠券库存
 */
@Data
public class CouponStockDTO {

    @ApiModelProperty("主键")
    private Long id; // 主键

    @ApiModelProperty("优惠券id")
    @NotNull(message = "优惠券id不能为空")
    private Long couponId;

    @ApiModelProperty("优惠券总量")
    @Range(max = 5000, min = 1, message = "优惠券总量必须在1~5000")
    private Integer totalNum; // 商品分桶初始库存

    private LocalDateTime createTime; // 创建时间

    private LocalDateTime updateTime; // 更新时间

    private Long creater; // 创建人

    private Long updater; // 更新人


}
