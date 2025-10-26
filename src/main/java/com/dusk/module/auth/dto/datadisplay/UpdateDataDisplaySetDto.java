package com.dusk.module.auth.dto.datadisplay;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 〈〉
 *
 * @author kefuming
 * @create 2022/2/8
 * @since 1.0.0
 */
@Getter
@Setter
public class UpdateDataDisplaySetDto {

    @Schema(description = "类型")
    private String displayType;
}
