package com.dusk.module.auth.dimension.dto;

import com.dusk.common.core.dto.EntityDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 维度操作日志响应DTO
 *
 * @author dusk
 */
@Getter
@Setter
@Schema(description = "维度操作日志响应")
public class DimensionOperationLogDto extends EntityDto {

    @Schema(description = "操作类型")
    private Integer operationType;

    @Schema(description = "操作类型名称")
    private String operationTypeName;

    @Schema(description = "操作目标类型")
    private String targetType;

    @Schema(description = "操作目标ID")
    private Long targetId;

    @Schema(description = "操作目标名称")
    private String targetName;

    @Schema(description = "操作详情")
    private String operationDetail;

    @Schema(description = "操作用户ID")
    private Long operatorId;

    @Schema(description = "操作用户名称")
    private String operatorName;

    @Schema(description = "客户端IP")
    private String clientIp;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
