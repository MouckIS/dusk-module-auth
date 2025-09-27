package com.dusk.module.ddm.module.auth.dto.face;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author kefuming
 * @date 2023/10/19 13:54
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ZipFileDto {

    /**
     * 文件名，带后缀
     */
    @ApiModelProperty("文件名，带后缀")
    private String fileName;

    /**
     * 文件类型
     */
    @ApiModelProperty("文件类型")
    private String fileType;

    /**
     * 文件字节
     */
    @ApiModelProperty("文件字节")
    private byte[] fileBytes;
}
