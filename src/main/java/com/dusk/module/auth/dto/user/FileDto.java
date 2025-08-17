package com.dusk.module.auth.dto.user;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;


/**
 * @author kefuming
 * @date 2020/5/18 11:54
 */
@Data
public class FileDto {
    @ApiModelProperty("文件名")
    private String fileName;
    @ApiModelProperty("文件类型")
    private String fileType;
    private String fileToken;

    public FileDto(){}
    public FileDto(String fileName, String fileType){
        this.fileName = fileName;
        this.fileType = fileType;
    }
}
