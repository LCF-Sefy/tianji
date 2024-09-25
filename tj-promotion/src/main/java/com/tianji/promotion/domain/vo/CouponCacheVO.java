package com.tianji.promotion.domain.vo;

import com.tianji.common.utils.DateUtils;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.enums.DiscountType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 保存在缓存中的优惠券实体
 */
@Data
@ApiModel(description = "缓存中优惠券信息——用户查询")
public class CouponCacheVO {
    @ApiModelProperty("优惠券id，新增不需要添加，更新必填")
    private Long id;
    @ApiModelProperty("优惠券名称")
    private String name;
    @ApiModelProperty("是否限定使用范围")
    private Boolean specific;

    @ApiModelProperty("优惠券类型，1：每满减，2：折扣，3：无门槛，4：普通满减")
    private DiscountType discountType;
    @ApiModelProperty("折扣门槛，0代表无门槛")
    private Integer thresholdAmount;
    @ApiModelProperty("折扣值，满减填抵扣金额；打折填折扣值：80标示打8折")
    private Integer discountValue;
    @ApiModelProperty("最大优惠金额")
    private Integer maxDiscountAmount;

    @ApiModelProperty("有效天数")
    private Integer termDays;
    @ApiModelProperty("使用有效期结束时间")
    private LocalDateTime termEndTime;

    @ApiModelProperty("是否可以领取")
    private Boolean available;

    @ApiModelProperty("是否可以使用")
    private Boolean received;

    @ApiModelProperty(value = "每个人限领的数量，默认1")
    private Integer userLimit;

    @ApiModelProperty(value = "总数量，不超过5000")
    private Integer totalNum;

    @ApiModelProperty(value = "开始发放时间")
    private LocalDateTime issueBeginTime;

    @ApiModelProperty(value = "结束发放时间")
    private LocalDateTime issueEndTime;


    /**
     * 将Coupon对象转换为Map
     * @return
     */
    // 反射实现将对象转换为Map<String, String>
    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<>();

        // 获取当前对象的所有字段
        Field[] fields = this.getClass().getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true); // 确保可以访问私有字段

            try {
                Object value = field.get(this); // 获取字段的值

                if (value != null) {
                    if (value instanceof LocalDateTime) {
                        // LocalDateTime类型转换为时间戳（毫秒）
                        map.put(field.getName(), String.valueOf(DateUtils.toEpochMilli(((LocalDateTime) value))));
                    } else {
                        // 其他类型直接转换为字符串
                        map.put(field.getName(), value.toString());
                    }
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace(); // 捕获异常并打印
            }
        }

        return map;
    }
}
