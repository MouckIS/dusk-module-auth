package com.dusk.module.auth.dto.station;

import com.dusk.common.core.dto.PagedAndSortedInputDto;
import com.dusk.common.core.enums.EUnitType;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;

/**
 * 获取未分配的厂站用户
 *
 * @author kefuming
 * @date 2022/09/23 16:32
 */
@Getter
@Setter
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
