package com.dusk.module.auth.dimension.dto;

import com.dusk.common.core.dto.PagedAndSortedInputDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 维度操作日志分页查询DTO
 *
 * @author dusk
 */
@Getter
@Setter
@Schema(description = "维度操作日志分页查询")
public class DimensionOperationLogPagedInputDto extends PagedAndSortedInputDto {

    @Schema(description = "操作类型")
    private Integer operationType;

    @Schema(description = "操作目标类型")
    private String targetType;

    @Schema(description = "操作用户ID")
    private Long operatorId;

    @Schema(description = "操作用户名称（模糊搜索）")
    private String operatorName;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;
}
