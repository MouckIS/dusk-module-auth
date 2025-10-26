package com.dusk.module.auth.dto.orga;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author kefuming
 * @date 2020-05-13 16:12
 */
@Getter
@Setter
public class UsersToOrganizationUnitInput {
    @Schema(description = "用户id列表")
    private List<Long> userIds = new ArrayList<>();

    @Schema(description = "组织机构id")
    private Long organizationUnitId;
}
