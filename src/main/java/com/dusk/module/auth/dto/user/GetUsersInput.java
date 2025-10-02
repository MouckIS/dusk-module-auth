package com.dusk.module.auth.dto.user;

import com.dusk.common.core.enums.EUnitType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import com.dusk.common.core.dto.PagedAndSortedInputDto;
import com.dusk.common.core.entity.BaseEntity;
import org.springframework.data.domain.Sort;

/**
 * @author kefuming
 * @date 2020/5/15 11:26
 */
@Data
@ApiModel(value = "GetUsersInput", description = "查询用户列表的实体类")
public class GetUsersInput extends PagedAndSortedInputDto {
    @ApiModelProperty("模糊查找[姓名、账号、电子邮箱、手机号、角色名、工号]")
    public String filter;
    @ApiModelProperty("角色id")
    public Long roleId;
    @ApiModelProperty("角色code")
    public String roleCode;
    @ApiModelProperty("角色 name")
    public String roleName;
    @ApiModelProperty("权限代码")
    public String permission;
    @ApiModelProperty("只显示锁定用户")
    private boolean onlyLockedUsers;
    @ApiModelProperty("账号类型")
    private EUnitType userType = EUnitType.Inner;
    @ApiModelProperty("显示离职账号")
    private boolean displayDimissionUsers;
    @Override
    protected Sort getSort() {
        if (StringUtils.isBlank(sorting)) {
            return Sort.by(BaseEntity.Fields.id);
        }
        return super.getSort();
    }
}
