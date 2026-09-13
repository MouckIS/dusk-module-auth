package com.dusk.module.auth.dto.user;

import com.dusk.common.core.dto.PagedAndSortedInputDto;
import com.dusk.common.core.entity.BaseEntity;
import com.dusk.common.core.enums.EUnitType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;

/**
 * @author kefuming
 * @date 2020/5/15 11:26
 */
@Getter
@Setter
@Schema(description = "查询用户列表的实体类")
public class GetUsersInput extends PagedAndSortedInputDto {
    @Schema(description = "模糊查找[姓名、账号、电子邮箱、手机号、角色名、工号]")
    public String filter;
    @Schema(description = "角色id")
    public Long roleId;
    @Schema(description = "角色code")
    public String roleCode;
    @Schema(description = "角色 name")
    public String roleName;
    @Schema(description = "权限代码")
    public String permission;
    @Schema(description = "只显示锁定用户")
    private boolean onlyLockedUsers;
    @Schema(description = "账号类型")
    private EUnitType userType = EUnitType.Inner;
    @Schema(description = "显示离职账号")
    private boolean displayDimissionUsers;

    @Override
    protected Sort getSort() {
        if (StringUtils.isBlank(sorting)) {
            return Sort.by(BaseEntity.Fields.id);
        }
        return super.getSort();
    }
}
