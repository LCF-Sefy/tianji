package com.tianji.stock.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 优惠券库存
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("coupon_stock")
@ApiModel(value = "stock对象", description = "优惠券的库存信息")
public class CouponStock {

    @ApiModelProperty("主键")
    private Long id; // 主键

    @ApiModelProperty("优惠券id")
    private Long couponId;

    @ApiModelProperty("优惠券总库存")
    private Integer totalNum;

    @ApiModelProperty("优惠券已领取数量")
    private Integer issueNum;

    private LocalDateTime createTime; // 创建时间

    private LocalDateTime updateTime; // 更新时间

    private Long creater; // 创建人

    private Long updater; // 更新人

}
