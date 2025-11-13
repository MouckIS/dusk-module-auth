package com.dusk.module.auth.dto.station;

import com.dusk.common.core.dto.PagedAndSortedInputDto;
import com.dusk.common.core.enums.EUnitType;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "厂站id")
    @NotNull(message = "厂站id不能为空")
    private Long stationId;

    @Schema(description = "搜索关键字(姓名/账号)")
    private String filter;

    @Schema(description = "用户类型")
    private EUnitType userType;

    @Override
    protected Sort getSort() {
        if (StringUtils.isBlank(sorting)) {
            return Sort.unsorted();
        }
        String sortingStr = "u." + sorting;
        ;
        return Sort.by(sortingDirection, sortingStr);
    }
}
