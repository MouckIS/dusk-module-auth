package com.dusk.module.ddm.module.auth.dto.sysno;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.dusk.common.core.dto.EntityDto;
import com.dusk.common.module.auth.enums.EnumResetType;

import java.time.LocalDateTime;

/**
 * @author kefuming
 * @date 2021-11-24 14:25
 */
@Getter
@Setter
public class SerialNoDto extends EntityDto {
    @ApiModelProperty("单据类型")
    private String billType;
    @ApiModelProperty("重置类型")
    private EnumResetType resetType;
    @ApiModelProperty("当前序号")
    private long currentNo;
    @ApiModelProperty("最后一次的序列号")
    private String lastNo;
    @ApiModelProperty("日期格式化")
    private String dateFormat;
    @ApiModelProperty("自增长序号长度")
    private int noLength;
    @ApiModelProperty("上一次获取序列号时间")
    private LocalDateTime lastUpdateTime;
}
