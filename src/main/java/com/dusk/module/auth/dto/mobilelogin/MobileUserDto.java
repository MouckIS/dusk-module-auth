package com.dusk.module.auth.dto.mobilelogin;

import cn.hutool.core.util.StrUtil;
import com.github.dozermapper.core.Mapping;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import com.dusk.common.core.dto.EntityDto;

/**
 * @author pengmengjiang
 * @date 2020/10/14 11:20
 */
@Data
@FieldNameConstants
public class MobileUserDto extends EntityDto {
    @Schema(description = "姓名")
    private String name;
    @Schema(description = "用户名")
    private String userName;
    @Schema(description = "租户id")
    private Long tenantId;
    @Mapping("tenant.name")
    @Schema(description = "租户显示名称")
    private String tenant;
    @Mapping("tenant.tenantName")
    @Schema(description = "租户代码")
    private String tenantName;
    @Schema(description = "登陆token")
    private String token;

    public String getTenant() {
        if (StrUtil.isEmpty(this.tenant)) {
            return "宿主";
        } else
            return this.tenant;
    }
}
