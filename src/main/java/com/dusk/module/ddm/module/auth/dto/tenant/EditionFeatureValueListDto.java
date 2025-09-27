package com.dusk.module.ddm.module.auth.dto.tenant;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import com.dusk.module.auth.dto.feature.FeatureValueInput;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.List;

/**
 * @author kefuming
 * @version 0.0.1
 * @date 2020/5/6 16:00
 */
@Data
@ApiModel(description = "版本特性列表")
public class EditionFeatureValueListDto implements Serializable {
    @NotBlank(message = "editionId不能为空")
    @ApiModelProperty(value = "版本id")
    public String editionId;
    @ApiModelProperty("特性列表")
    public List<FeatureValueInput> featureValueList;
}
