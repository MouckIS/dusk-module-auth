package com.dusk.module.ddm.module.auth.dto.station;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import com.dusk.common.core.dto.PagedAndSortedInputDto;
import com.dusk.common.core.entity.BaseEntity;
import com.dusk.common.core.entity.TreeEntity;
import com.dusk.common.module.auth.enums.EUnitType;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

/**
 * @author kefuming
 * @date  2022/9/21 20:46
 */
@Data
public class GetStationUsersInput extends PagedAndSortedInputDto {
    @ApiModelProperty("厂站id")
    private List<Long> stationIds = new ArrayList<>();

    @ApiModelProperty("搜索关键字(姓名/账号)")
    private String filter;

    @ApiModelProperty("深度查询(即包括子节点的人员, 默认true)")
    private boolean deepQuery = true;

    @ApiModelProperty("用户类型")
    private EUnitType userType;

    @Override
    protected Sort getSort() {
        if(StringUtils.isBlank(sorting)){
            return Sort.unsorted();
        }
        String sortingStr = sorting;

        if(StationUserListDto.Fields.stationId.equals(sorting)){
            sortingStr = BaseEntity.Fields.id;
        }else if(StationUserListDto.Fields.stationName.equals(sorting)){
            sortingStr = TreeEntity.Fields.displayName;
        }else{
            sortingStr = "u." + sorting;
        }

        return Sort.by(sortingDirection, sortingStr);
    }
}
