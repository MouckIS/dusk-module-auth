package com.dusk.module.auth.dto.configuration;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import com.dusk.module.auth.dto.feature.FeatureValueInput;

import java.util.List;

/**
 * @author kefuming
 * @date 2020/5/13 11:43
 */
@Data
public class TenantFeatureInputDto {
    @ApiModelProperty("租户id")
    Long tenantId;
    @ApiModelProperty("特性")
    List<FeatureValueInput> featureList;
}
