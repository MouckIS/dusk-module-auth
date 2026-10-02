package com.dusk.module.auth.dto.face;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2023/10/19 13:54
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ZipFileDto {

    /**
     * 文件名，带后缀
     */
    @Schema(description = "文件名，带后缀")
    private String fileName;

    /**
     * 文件类型
     */
    @Schema(description = "文件类型")
    private String fileType;

    /**
     * 文件字节
     */
    @Schema(description = "文件字节")
    private byte[] fileBytes;
}
