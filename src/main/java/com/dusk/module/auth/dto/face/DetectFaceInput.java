package com.dusk.module.auth.dto.face;

import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class DetectFaceInput {
    @ApiModelProperty("人脸图片base64")
    @NotBlank(message = "照片不能为空")
    private String imgBase64;
}
