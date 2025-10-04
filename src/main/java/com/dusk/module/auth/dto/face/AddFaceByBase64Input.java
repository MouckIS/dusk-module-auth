package com.dusk.module.auth.dto.face;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import com.dusk.common.core.dto.ImgBase64Dto;

import javax.validation.constraints.NotNull;

/**
 * @author kefuming
 * @date 2021-05-21 8:44
 */
@Getter
@Setter
public class AddFaceByBase64Input extends ImgBase64Dto {
    @Schema(description = "用户id")
    @NotNull(message = "用户id不能为空！")
    private Long userId;

    @Schema(description = "需要移除的用户id")
    private Long removeUserId;
}
