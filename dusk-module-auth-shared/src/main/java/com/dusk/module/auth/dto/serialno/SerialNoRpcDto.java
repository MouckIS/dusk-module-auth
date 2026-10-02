package com.dusk.module.auth.dto.serialno;

import com.dusk.common.core.entity.FullAuditedEntity;
import com.dusk.module.auth.enums.EnumResetType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * @author kefuming
 * @date 2022/12/05 8:37
 */
@Getter
@Setter
public class SerialNoRpcDto extends FullAuditedEntity {

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

    @Schema(description = "序列化长度")
    private int noLength;

    @Schema(description = "最后更新时间")
    private LocalDateTime lastUpdateTime;
}
