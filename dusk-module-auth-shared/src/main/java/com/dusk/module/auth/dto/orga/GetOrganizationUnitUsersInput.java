package com.dusk.module.auth.dto.orga;

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
 * @author pengjian
 * @date 2020-05-13 14:42
 */
@Getter
@Setter
public class GetOrganizationUnitUsersInput extends PagedAndSortedInputDto {
    @Schema(description = "组织机构id")
    private List<Long> organizationUnitIds = new ArrayList<>();

    @Schema(description = "搜索关键字(姓名/账号)")
    private String filter;

    @Schema(description = "深度查询(即包括子节点的人员, 默认true)")
    private boolean deepQuery = true;

    @Schema(description = "组织机构的类型")
    private EUnitType type;

    @Schema(description = "专业编码")
    private String specialityCode;

    @Schema(description = "人员id")
    private List<Long> userIdList;

    @Override
    protected Sort getSort() {
        if (StringUtils.isBlank(sorting)) {
            return getDefaultSort();
        }

        if (OrganizationUnitUserListDto.Fields.organizationUnitId.equals(sorting)) {
            sorting = BaseEntity.Fields.id;
        } else if (OrganizationUnitUserListDto.Fields.organizationUnitName.equals(sorting)) {
            sorting = TreeEntity.Fields.displayName;
        } else {
            sorting = "u." + sorting;
        }

        return Sort.by(sortingDirection, sorting);
    }
}
