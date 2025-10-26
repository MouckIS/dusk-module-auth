package com.dusk.module.auth.dto.sysno;

import com.dusk.common.core.dto.EntityDto;
import com.dusk.common.rpc.auth.enums.EnumResetType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * @author kefuming
 * @date 2021-11-24 14:25
 */
@Getter
@Setter
public class SerialNoDto extends EntityDto {
    @Schema(description = "单据类型")
    private String billType;
    @Schema(description = "重置类型")
    private EnumResetType resetType;
    @Schema(description = "当前序号")
    private long currentNo;
    @Schema(description = "最后一次的序列号")
    private String lastNo;
    @Schema(description = "日期格式化")
    private String dateFormat;
    @Schema(description = "自增长序号长度")
    private int noLength;
    @Schema(description = "上一次获取序列号时间")
    private LocalDateTime lastUpdateTime;
}
