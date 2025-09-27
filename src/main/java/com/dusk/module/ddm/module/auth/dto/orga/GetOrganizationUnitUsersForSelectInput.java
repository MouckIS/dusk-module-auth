package com.dusk.module.ddm.module.auth.dto.orga;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import com.dusk.common.core.dto.PagedAndSortedInputDto;
import com.dusk.common.core.entity.BaseEntity;
import com.dusk.common.core.entity.TreeEntity;
import com.dusk.common.module.auth.dto.orga.OrganizationUnitUserListDto;
import org.springframework.data.domain.Sort;

/**
 * 获取组织机构下拉用户列表的输入Dto
 *
 * @author kefuming
 * @date 2020/10/16 16:32
 */
@Data
public class GetOrganizationUnitUsersForSelectInput extends PagedAndSortedInputDto {

    @ApiModelProperty("组织机构id")
    private Long orgId;

    @ApiModelProperty("搜索关键字(姓名/账号)")
    private String filter;

    @Override
    protected Sort getSort() {
        if (StringUtils.isBlank(sorting)) {
            return Sort.unsorted();
        }
        String sortingStr = sorting;

        if (OrganizationUnitUserListDto.Fields.organizationUnitId.equals(sorting)) {
            sortingStr = BaseEntity.Fields.id;
        } else if (OrganizationUnitUserListDto.Fields.organizationUnitName.equals(sorting)) {
            sortingStr = TreeEntity.Fields.displayName;
        } else {
            sortingStr = "u." + sorting;
        }

        return Sort.by(sortingDirection, sortingStr);
    }

}
