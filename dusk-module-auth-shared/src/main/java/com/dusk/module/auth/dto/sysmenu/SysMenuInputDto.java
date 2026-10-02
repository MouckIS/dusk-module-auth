package com.dusk.module.auth.dto.sysmenu;

import com.dusk.common.core.dto.EntityDto;
import com.dusk.module.auth.enums.MenuTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SysMenuInputDto extends EntityDto {

    private String title;

    private String routeName;

    private String iconType;

    private String iconClass;

    private Integer sortIndex;

    private String parentRouteName;
    @Schema(description = "是否屏蔽菜单")
    private Boolean ignored;

    @Enumerated(EnumType.STRING)
    private MenuTypeEnum type = MenuTypeEnum.SYSTEM;
}
