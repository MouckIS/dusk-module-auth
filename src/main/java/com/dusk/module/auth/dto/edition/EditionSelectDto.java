package com.dusk.module.auth.dto.edition;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import com.dusk.common.core.dto.EntityDto;
import com.dusk.module.auth.enums.SubscriptionPaymentGatewayType;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * @author kefuming
 * @date 2020-05-08 10:18
 */
@Data
public class EditionSelectDto extends EntityDto {
    @ApiModelProperty("名称")
    private String name;
    @ApiModelProperty("显示的名称")
    private String displayName;
    @ApiModelProperty("关联的过期版本id")
    private Integer expiringEditionId;
    @ApiModelProperty("每月的价格")
    private BigDecimal monthlyPrice;
    @ApiModelProperty("每年的价格")
    private BigDecimal annualPrice;
    @ApiModelProperty("试用天数")
    private Integer trialDayCount;
    @ApiModelProperty("过期后多少天执行某些操作")
    private Integer waitingDayAfterExpire;
    @ApiModelProperty("是否免费")
    private boolean isFree;
    @ApiModelProperty("额外数据")
    private Map<SubscriptionPaymentGatewayType, Map<String, String>> additionalData;

    public EditionSelectDto()
    {
        additionalData = new HashMap<SubscriptionPaymentGatewayType, Map<String, String>>();
    }
}
