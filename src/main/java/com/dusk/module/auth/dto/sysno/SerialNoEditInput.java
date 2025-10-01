package com.dusk.module.auth.dto.sysno;

import com.dusk.commom.rpc.auth.enums.EnumResetType;
import io.swagger.annotations.ApiModelProperty;
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
    @ApiModelProperty("主键")
    @NotNull(message = "数据主键不能为空")
    private Long id;
    @ApiModelProperty("重置类型")
    @NotNull(message = "重置类型不能为空")
    private EnumResetType resetType;
    @ApiModelProperty("当前序号")
    @Min(message = "当前序号不能小于0", value = 0)
    private long currentNo;
    @ApiModelProperty("日期格式化")
    private String dateFormat;
    @ApiModelProperty("自增长序号长度")
    @Min(message = "长度不能小于1", value = 1)
    @Max(message = "长度不能大于15", value = 15)
    private int noLength;
}
