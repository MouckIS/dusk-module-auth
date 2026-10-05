package com.dusk.module.auth.dto.orga;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author kefuming
 * @date 2021/8/4 10:09
 */
@Getter
public class BindRoleToOrgInput {

    @Schema(description = "角色id")
    @NotNull(message = "roleId不能为空")
    private Long roleId;

    @Schema(description = "组织id列表")
    private List<Long> orgIds = new ArrayList<>();

    @Schema(description = "是否包含子区域")
    private boolean includeChild;
}
