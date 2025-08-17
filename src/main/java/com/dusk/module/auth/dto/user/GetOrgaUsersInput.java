package com.dusk.module.auth.dto.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import com.dusk.common.framework.dto.PagedAndSortedInputDto;
import com.dusk.common.framework.entity.BaseEntity;
import org.springframework.data.domain.Sort;

import javax.validation.constraints.NotNull;

/**
 * @author kefuming
 * @date 2020/5/15 11:26
 */
@Data
@ApiModel(value = "GetUsersInput", description = "查询用户列表的实体类")
public class GetOrgaUsersInput extends PagedAndSortedInputDto {
    @ApiModelProperty("组织机构Id")
    @NotNull(message = "组织机构id不能为空")
    private Long orgaId;
    @ApiModelProperty("模糊查找[姓名、账号、电子邮箱、手机号、角色名、工号]")
    public String filter;
    @ApiModelProperty("角色id")
    public Long roleId;
    @ApiModelProperty("角色 name")
    public String roleName;

    @Override
    protected Sort getSort() {
        if (StringUtils.isBlank(sorting)) {
            return Sort.by(BaseEntity.Fields.id);
        }
        return super.getSort();
    }
}
