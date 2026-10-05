package com.dusk.module.auth.dto.orga;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2020-05-13 17:23
 */
@Getter
@Setter
public class ParentOrganizationOutput {
    @Schema(description = "组织机构id")
    private String id;

    @Schema(description = "组织机构名称")
    private String displayName;

    @Schema(description = "是否为厂站")
    private boolean station = false;

    private int sortId;
}
