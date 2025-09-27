package com.dusk.module.ddm.module.auth.dto.orga;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author kefuming
 * @date 2020-05-13 16:12
 */
@Data
public class UsersToOrganizationUnitInput {
    @ApiModelProperty("用户id列表")
    private List<Long> userIds = new ArrayList<>();

    @ApiModelProperty("组织机构id")
    private Long organizationUnitId;
}
