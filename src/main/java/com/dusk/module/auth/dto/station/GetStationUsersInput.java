package com.dusk.module.auth.dto.station;

import com.dusk.common.core.dto.PagedAndSortedInputDto;
import com.dusk.common.core.entity.BaseEntity;
import com.dusk.common.core.entity.TreeEntity;
import com.dusk.common.core.enums.EUnitType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

/**
 * @author kefuming
 * @date 2022/9/21 20:46
 */
@Getter
@Setter
public class GetStationUsersInput extends PagedAndSortedInputDto {
    @Schema(description = "厂站id")
    private List<Long> stationIds = new ArrayList<>();

    @Schema(description = "搜索关键字(姓名/账号)")
    private String filter;

    @Schema(description = "深度查询(即包括子节点的人员, 默认true)")
    private boolean deepQuery = true;

    @Schema(description = "用户类型")
    private EUnitType userType;

    @Override
    protected Sort getSort() {
        if (StringUtils.isBlank(sorting)) {
            return Sort.unsorted();
        }
        String sortingStr = sorting;

        if (StationUserListDto.Fields.stationId.equals(sorting)) {
            sortingStr = BaseEntity.Fields.id;
        } else if (StationUserListDto.Fields.stationName.equals(sorting)) {
            sortingStr = TreeEntity.Fields.displayName;
        } else {
            sortingStr = "u." + sorting;
        }

        return Sort.by(sortingDirection, sortingStr);
    }
}
