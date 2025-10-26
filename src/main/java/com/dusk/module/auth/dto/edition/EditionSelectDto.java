package com.dusk.module.auth.dto.edition;

import com.dusk.common.core.dto.EntityDto;
import com.dusk.module.auth.enums.SubscriptionPaymentGatewayType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * @author kefuming
 * @date 2020-05-08 10:18
 */
@Getter
@Setter
public class EditionSelectDto extends EntityDto {
    @Schema(description = "名称")
    private String name;
    @Schema(description = "显示的名称")
    private String displayName;
    @Schema(description = "关联的过期版本id")
    private Integer expiringEditionId;
    @Schema(description = "每月的价格")
    private BigDecimal monthlyPrice;
    @Schema(description = "每年的价格")
    private BigDecimal annualPrice;
    @Schema(description = "试用天数")
    private Integer trialDayCount;
    @Schema(description = "过期后多少天执行某些操作")
    private Integer waitingDayAfterExpire;
    @Schema(description = "是否免费")
    private boolean isFree;
    @Schema(description = "额外数据")
    private Map<SubscriptionPaymentGatewayType, Map<String, String>> additionalData;

    public EditionSelectDto()
    {
        additionalData = new HashMap<SubscriptionPaymentGatewayType, Map<String, String>>();
    }
}
