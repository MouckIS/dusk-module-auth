package com.dusk.module.auth.dto.feature;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * @author kefuming
 * @date 2020/5/13 11:47
 */
@Data
public class EditionFeatureInputDto {
    @Schema(description = "版本id")
    Long editionId;
    @Schema(description = "特性")
    List<FeatureValueInput> featureList;
}
