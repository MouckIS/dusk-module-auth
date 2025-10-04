package com.dusk.module.auth.dto.sysno;

import com.dusk.common.rpc.auth.enums.EnumResetType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * @author kefuming
 * @date 2021-11-24 15:06
 */
@Getter
@Setter
public class SerialNoEditInput {
    @Schema(description = "主键")
    @NotNull(message = "数据主键不能为空")
    private Long id;
    @Schema(description = "重置类型")
    @NotNull(message = "重置类型不能为空")
    private EnumResetType resetType;
    @Schema(description = "当前序号")
    @Min(message = "当前序号不能小于0", value = 0)
    private long currentNo;
    @Schema(description = "日期格式化")
    private String dateFormat;
    @Schema(description = "自增长序号长度")
    @Min(message = "长度不能小于1", value = 1)
    @Max(message = "长度不能大于15", value = 15)
    private int noLength;
}
