package com.dusk.module.auth.dto.fingerprint;

import com.dusk.common.core.dto.VersionDto;
import com.dusk.common.rpc.auth.enums.FingerprintFromEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2021-05-12 8:50
 */
@Getter
@Setter
public class SaveFingerprintInputDto extends VersionDto {
    @Schema(description = "用户id")
    @NotNull(message = "用户Id不能为空")
    private Long userId;

    @Schema(description = "用户序列")
    private Integer userSeq;

    @Schema(description = "指纹名称")
    @NotBlank(message = "指纹名称不能为空")
    private String name;

    @Schema(description = "指纹数据")
    @NotBlank(message = "指纹数据不能为空")
    private String data;

    @Schema(description = "指纹数据大小")
    private Integer size;

    @Schema(description = "指纹来源，默认为指纹采集器Live20R")
    private FingerprintFromEnum fromEnum = FingerprintFromEnum.LIVE20R;
}
