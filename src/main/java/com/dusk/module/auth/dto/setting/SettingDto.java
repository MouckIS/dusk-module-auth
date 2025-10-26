package com.dusk.module.auth.dto.setting;

import com.dusk.module.ddm.dto.ui.InputType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2020/11/27 10:50
 */
@Getter
@Setter
public class SettingDto {
    @Schema(description = "名称， 唯一标识")
    private String name;

    private String parentName;

    @Schema(description = "显示的名称")
    private String displayName;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "默认值")
    private String defaultValue;

    @Schema(description = "输入类型")
    private InputType inputType;

    @Schema(description = "配置值")
    private String value;

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "文件下载链接")
    private String downloadUrl;
}
