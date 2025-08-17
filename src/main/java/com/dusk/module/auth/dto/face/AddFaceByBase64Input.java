package com.dusk.module.auth.dto.face;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.dusk.common.framework.dto.ImgBase64Dto;

import javax.validation.constraints.NotNull;

/**
 * @author kefuming
 * @date 2021-05-21 8:44
 */
@Getter
@Setter
public class AddFaceByBase64Input extends ImgBase64Dto {
    @ApiModelProperty("用户id")
    @NotNull(message = "用户id不能为空！")
    private Long userId;

    @ApiModelProperty("需要移除的用户id")
    private Long removeUserId;
}
