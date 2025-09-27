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
public class TenantFeatureValueListDto implements Serializable {
    @NotBlank(message = "tenantId不能为空")
    @ApiModelProperty(value = "租户id")
    public String tenantId;
    @ApiModelProperty(value = "特性列表")
    public List<FeatureValueInput> featureValueList;
}
