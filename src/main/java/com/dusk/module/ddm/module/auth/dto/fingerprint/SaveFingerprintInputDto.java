package com.dusk.module.ddm.module.auth.dto.fingerprint;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.dusk.common.core.dto.VersionDto;
import com.dusk.common.module.auth.enums.FingerprintFromEnum;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @author kefuming
 * @date 2021-05-12 8:50
 */
@Getter
@Setter
public class SaveFingerprintInputDto extends VersionDto {
    @ApiModelProperty(value = "用户id")
    @NotNull(message = "用户Id不能为空")
    private Long userId;

    @ApiModelProperty(value = "用户序列")
    private Integer userSeq;

    @ApiModelProperty("指纹名称")
    @NotBlank(message = "指纹名称不能为空")
    private String name;

    @ApiModelProperty("指纹数据")
    @NotBlank(message = "指纹数据不能为空")
    private String data;

    @ApiModelProperty("指纹数据大小")
    private Integer size;

    @ApiModelProperty("指纹来源，默认为指纹采集器Live20R")
    private FingerprintFromEnum fromEnum = FingerprintFromEnum.LIVE20R;
}
