package com.dusk.module.auth.dto.setting;

import com.dusk.module.ddm.dto.ui.InputType;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2020/11/27 10:50
 */
@Getter
@Setter
public class SettingDto {
    @ApiModelProperty("名称， 唯一标识")
    private String name;

    private String parentName;

    @ApiModelProperty("显示的名称")
    private String displayName;

    @ApiModelProperty("描述")
    private String description;

    @ApiModelProperty("默认值")
    private String defaultValue;

    @ApiModelProperty("输入类型")
    private InputType inputType;

    @ApiModelProperty("配置值")
    private String value;

    @ApiModelProperty("文件名")
    private String fileName;

    @ApiModelProperty("文件下载链接")
    private String downloadUrl;
}
