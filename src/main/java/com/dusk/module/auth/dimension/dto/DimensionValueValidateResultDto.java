package com.dusk.module.auth.dimension.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 维度值校验结果DTO
 *
 * @author dusk
 */
@Getter
@Setter
@Schema(description = "维度值校验结果")
public class DimensionValueValidateResultDto implements Serializable {

    @Schema(description = "是否全部有效")
    private boolean valid;

    @Schema(description = "有效的维度值编码")
    private List<String> validCodes;

    @Schema(description = "无效的维度值编码")
    private List<String> invalidCodes;

    @Schema(description = "失败消息列表")
    private List<String> messages;
}
