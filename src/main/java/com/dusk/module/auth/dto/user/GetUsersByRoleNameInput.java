package com.dusk.module.auth.dto.user;

import com.dusk.common.core.enums.EUnitType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.dusk.common.core.dto.PagedAndSortedInputDto;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * @author kefuming
 * @date 2020-12-10 9:13
 */
@Getter
@Setter
@ApiModel
public class GetUsersByRoleNameInput extends PagedAndSortedInputDto {
    @ApiModelProperty("角色名称列表")
    @NotEmpty(message = "角色名称不能为空")
    public List<String> roleNames;
    @ApiModelProperty("账号类型")
    private EUnitType userType;
}
