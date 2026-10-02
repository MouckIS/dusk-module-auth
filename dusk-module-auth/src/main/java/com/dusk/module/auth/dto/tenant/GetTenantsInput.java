package com.dusk.module.auth.dto.tenant;

import com.dusk.common.core.dto.PagedAndSortedInputDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * @author kefuming
 * @date 2020-05-06 16:38
 */
@Getter
@Setter
@Schema(description = "查询租户列表实体类")
public class GetTenantsInput extends PagedAndSortedInputDto {
    @Schema(description = "根据租户名模糊查找")
    private String filter;
    @Schema(description = "订阅到期区间起始时间")
    private LocalDateTime subscriptionEndDateStart;
    @Schema(description = "订阅到期区间结束时间")
    private LocalDateTime subscriptionEndDateEnd;
    @Schema(description = "创建时间区间起始时间")
    private LocalDateTime creationDateStart;
    @Schema(description = "创建时间区间结束时间")
    private LocalDateTime creationDateEnd;
    @Schema(description = "租户关联的版本的唯一标识")
    private String editionId;
}
