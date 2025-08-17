package com.dusk.module.auth.dto.station;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import com.dusk.common.framework.dto.PagedAndSortedInputDto;
import com.dusk.common.module.auth.enums.EUnitType;
import org.springframework.data.domain.Sort;

import javax.validation.constraints.NotNull;

/**
 * 获取未分配的厂站用户
 *
 * @Author kefuming
 * @Date 2022/09/23 16:32
 */
@Data
public class GetNotAssignedStationUsersInput extends PagedAndSortedInputDto {

    @ApiModelProperty("厂站id")
    @NotNull(message = "厂站id不能为空")
    private Long stationId;

    @ApiModelProperty("搜索关键字(姓名/账号)")
    private String filter;

    @ApiModelProperty("用户类型")
    private EUnitType userType;

    @Override
    protected Sort getSort() {
        if (StringUtils.isBlank(sorting)) {
            return Sort.unsorted();
        }
        String sortingStr = "u." + sorting;;
        return Sort.by(sortingDirection, sortingStr);
    }
}
