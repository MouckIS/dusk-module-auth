package com.dusk.module.auth.dto.face;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
@AllArgsConstructor
public class DetectFaceInput {
    @Schema(description = "人脸图片base64")
    @NotBlank(message = "照片不能为空")
    private String imgBase64;
}
