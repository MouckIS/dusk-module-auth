package com.dusk.module.auth.dto.configuration;

import com.dusk.module.auth.dto.feature.FeatureValueInput;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author kefuming
 * @date 2020/5/13 11:43
 */
@Getter
@Setter
public class TenantFeatureInputDto {
    @Schema(description = "租户id")
    Long tenantId;
    @Schema(description = "特性")
    List<FeatureValueInput> featureList;
}
