package com.dusk.module.auth.dto.edition;

import com.dusk.common.core.dto.VersionDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * @author kefuming
 * @date 2020-05-08 10:13
 */
@Getter
@Setter
public class EditionEditDto extends VersionDto {
    @NotBlank(message = "名称不能为空")
    @Schema(description = "显示的名称")
    private String displayName;

    @Schema(description = "每月的价格")
    private BigDecimal monthlyPrice;

    @Schema(description = "每年的价格")
    private BigDecimal annualPrice;

    @Schema(description = "试用天数")
    private Integer trialDayCount;

    @Schema(description = "过期后多少天执行某些操作")
    private Integer waitingDayAfterExpire;

    @Schema(description = "关联的过期版本id")
    private String expiringEditionId;
}
