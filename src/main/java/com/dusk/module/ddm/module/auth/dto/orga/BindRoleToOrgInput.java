package com.dusk.module.ddm.module.auth.dto.orga;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.Getter;
import org.w3c.dom.stylesheets.LinkStyle;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author kefuming
 * @date 2021/8/4 10:09
 */
@Getter
public class BindRoleToOrgInput {

    @ApiModelProperty("角色id")
    @NotNull(message = "roleId不能为空")
    private Long roleId;

    @ApiModelProperty("组织id列表")
    private List<Long> orgIds = new ArrayList<>();

    @ApiModelProperty("是否包含子区域")
    private boolean includeChild;
}
