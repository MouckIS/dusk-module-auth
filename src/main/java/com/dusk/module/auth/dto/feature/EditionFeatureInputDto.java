package com.dusk.module.auth.dto.feature;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @author kefuming
 * @date 2020/5/13 11:47
 */
@Data
public class EditionFeatureInputDto {
    @ApiModelProperty("版本id")
    Long editionId;
    @ApiModelProperty("特性")
    List<FeatureValueInput> featureList;
}
