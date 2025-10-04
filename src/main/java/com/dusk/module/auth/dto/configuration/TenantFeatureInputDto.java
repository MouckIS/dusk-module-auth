package com.dusk.module.auth.dto.configuration;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.dusk.module.auth.dto.feature.FeatureValueInput;

import java.util.List;

/**
 * @author kefuming
 * @date 2020/5/13 11:43
 */
@Data
public class TenantFeatureInputDto {
    @Schema(description = "租户id")
    Long tenantId;
    @Schema(description = "特性")
    List<FeatureValueInput> featureList;
}
