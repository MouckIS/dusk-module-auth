package com.dusk.module.auth.dto.mobilelogin;

import cn.hutool.core.util.StrUtil;
import com.github.dozermapper.core.Mapping;
import io.swagger.annotations.ApiModelProperty;
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
    @ApiModelProperty("姓名")
    private String name;
    @ApiModelProperty("用户名")
    private String userName;
    @ApiModelProperty("租户id")
    private Long tenantId;
    @Mapping("tenant.name")
    @ApiModelProperty("租户显示名称")
    private String tenant;
    @Mapping("tenant.tenantName")
    @ApiModelProperty("租户代码")
    private String tenantName;
    @ApiModelProperty("登陆token")
    private String token;

    public String getTenant() {
        if (StrUtil.isEmpty(this.tenant)) {
            return "宿主";
        } else
            return this.tenant;
    }
}
