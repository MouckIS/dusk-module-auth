package com.dusk.module.auth.dto.tenant;

import com.dusk.module.auth.dto.feature.FeatureValueInput;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * @author kefuming
 * @version 0.0.1
 * @date 2020/5/6 16:00
 */
@Getter
@Setter
@ApiModel(description = "版本特性列表")
public class EditionFeatureValueListDto implements Serializable {
    @NotBlank(message = "editionId不能为空")
    @ApiModelProperty(value = "版本id")
    public String editionId;
    @ApiModelProperty("特性列表")
    public List<FeatureValueInput> featureValueList;
}
