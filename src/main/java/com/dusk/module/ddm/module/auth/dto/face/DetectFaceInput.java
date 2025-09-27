package com.dusk.module.ddm.module.auth.dto.face;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
@AllArgsConstructor
public class DetectFaceInput {
    @ApiModelProperty("人脸图片base64")
    @NotBlank(message = "照片不能为空")
    private String imgBase64;
}
