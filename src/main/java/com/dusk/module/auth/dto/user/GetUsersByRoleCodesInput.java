package com.dusk.module.auth.dto.user;

import com.dusk.common.core.enums.EUnitType;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "通过角色code列表查询用户的实体类")
public class GetUsersByRoleCodesInput extends PagedAndSortedInputDto {
    @Schema(description = "角色code列表")
    public List<String> roleCodeList;
    @Schema(description = "是否满足所有角色")
    private boolean allRoles;
    @Schema(description = "账号类型")
    private EUnitType userType;

    @Override
    protected Sort getSort() {
        if (StringUtils.isBlank(sorting)) {
            return Sort.by(BaseEntity.Fields.id);
        }
        return super.getSort();
    }
}
