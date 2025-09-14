package com.dusk.module.auth.dto.edition;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import com.dusk.common.framework.dto.VersionDto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

/**
 * @author kefuming
 * @date 2020-05-08 10:13
 */
@Data
public class EditionEditDto extends VersionDto {
    @NotBlank(message = "名称不能为空")
    @ApiModelProperty("显示的名称")
    private String displayName;

    @ApiModelProperty("每月的价格")
    private BigDecimal monthlyPrice;

    @ApiModelProperty("每年的价格")
    private BigDecimal annualPrice;

    @ApiModelProperty("试用天数")
    private Integer trialDayCount;

    @ApiModelProperty("过期后多少天执行某些操作")
    private Integer waitingDayAfterExpire;

    @ApiModelProperty("关联的过期版本id")
    private String expiringEditionId;
}
