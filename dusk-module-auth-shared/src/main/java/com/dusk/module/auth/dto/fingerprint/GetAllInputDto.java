package com.dusk.module.auth.dto.fingerprint;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author kefuming
 * @date 2021-05-12 9:37
 */
@Getter
@Setter
public class GetAllInputDto implements Serializable {
    @Schema(description = "用户id")
    private List<Long> userIds = new ArrayList<>();

    @Schema(description = "指纹id")
    private String fingerprintId;

    @Schema(description = "指纹名过滤")
    private String filter;
}
