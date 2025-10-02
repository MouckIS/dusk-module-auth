package com.dusk.module.auth.dto.user;

import com.dusk.common.core.enums.EUnitType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import com.dusk.common.core.dto.PagedAndSortedInputDto;
import com.dusk.common.core.entity.BaseEntity;
import org.springframework.data.domain.Sort;

import java.util.List;

/**
 * @author kefuming
 * @date 2020/10/27
 */
@Data
@ApiModel(value = "GetUsersByRoleCodesInput", description = "通过角色code列表查询用户的实体类")
public class GetUsersByRoleCodesInput extends PagedAndSortedInputDto {
    @ApiModelProperty("角色code列表")
    public List<String> roleCodeList;
    @ApiModelProperty("是否满足所有角色")
    private boolean allRoles;
    @ApiModelProperty("账号类型")
    private EUnitType userType;

    @Override
    protected Sort getSort() {
        if (StringUtils.isBlank(sorting)) {
            return Sort.by(BaseEntity.Fields.id);
        }
        return super.getSort();
    }
}
