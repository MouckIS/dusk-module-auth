package com.dusk.module.ddm.module.auth.dto.tenant;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import com.dusk.common.core.dto.PagedAndSortedInputDto;

import java.time.LocalDateTime;

/**
 * @author kefuming
 * @date 2020-05-06 16:38
 */
@Data
@ApiModel(value = "GetTenantsInput", description = "查询租户列表实体类")
public class GetTenantsInput extends PagedAndSortedInputDto {
    @ApiModelProperty("根据租户名模糊查找")
    private String filter;
    @ApiModelProperty("订阅到期区间起始时间")
    private LocalDateTime subscriptionEndDateStart;
    @ApiModelProperty("订阅到期区间结束时间")
    private LocalDateTime subscriptionEndDateEnd;
    @ApiModelProperty("创建时间区间起始时间")
    private LocalDateTime creationDateStart;
    @ApiModelProperty("创建时间区间结束时间")
    private LocalDateTime creationDateEnd;
    @ApiModelProperty("租户关联的版本的唯一标识")
    private String editionId;
}
