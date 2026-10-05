package com.dusk.module.auth.dto.sysmenu;

import com.dusk.common.core.dto.EntityDto;
import com.dusk.module.auth.enums.MenuTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetSysMenuListDto extends EntityDto {


    @Schema(description = "菜单名")
    private String title;

    @Schema(description = "前端路由名")
    private String routeName;

    @Schema(description = "图标类型")
    private String iconType;

    @Schema(description = "图标class")
    private String iconClass;

    @Schema(description = "排序")
    private Integer sortIndex;

    @Schema(description = "路由的name")
    private String parentRouteName;

    @Schema(description = "是否屏蔽菜单")
    private Boolean ignored;

    @Schema(description = "菜单类型")
    @Enumerated(EnumType.STRING)
    private MenuTypeEnum type;

    public MenuTypeEnum getType() {
        return type == null ? MenuTypeEnum.SYSTEM : type;
    }

    public Boolean getIgnored() {
        return ignored != null && ignored;
    }

    public void setIgnored(Boolean ignored) {
        this.ignored = ignored != null && ignored;
    }
}
