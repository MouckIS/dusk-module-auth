package com.dusk.module.auth.dto.orga;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.dusk.common.module.auth.dto.orga.GetOrganizationUnitUsersInput;
import org.springframework.data.domain.Sort;

/**
 * GetOrganizationUnitUsersInput 的扩展类， 添加离职字段
 *
 * @Author kefuming
 * @CreateTime 2023/8/8
 */
@Getter
@Setter
public class GetOrganizationUnitUsersExtInput extends GetOrganizationUnitUsersInput {
    @ApiModelProperty("显示离职账号")
    private boolean displayDimissionUsers = false;

    @Override
    protected Sort getSort() {
        return Sort.unsorted();
    }
}
