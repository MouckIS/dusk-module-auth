package com.dusk.module.auth.dto.edition;

import com.dusk.common.core.dto.EntityDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * @author kefuming
 * @date 2020-05-08 10:16
 */
@Getter
@Setter
public class EditionListDto extends EntityDto {
    @Schema(description = "名称")
    private String name;
    @Schema(description = "显示的名称")
    private String displayName;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
