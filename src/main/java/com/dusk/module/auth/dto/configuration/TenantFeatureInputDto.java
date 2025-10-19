package com.dusk.module.auth.dto.configuration;

import com.dusk.module.auth.dto.feature.FeatureValueInput;
import io.swagger.annotations.ApiModelProperty;
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
    @ApiModelProperty("租户id")
    Long tenantId;
    @ApiModelProperty("特性")
    List<FeatureValueInput> featureList;
}
