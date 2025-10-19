package com.dusk.module.auth.dto.mobilelogin;

import cn.hutool.core.util.StrUtil;
import com.dusk.common.core.dto.EntityDto;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * @author pengmengjiang
 * @date 2020/10/14 11:20
 */
@Getter
@Setter
@FieldNameConstants
public class MobileUserDto extends EntityDto {
    @ApiModelProperty("姓名")
    private String name;
    @ApiModelProperty("用户名")
    private String userName;
    @ApiModelProperty("租户id")
    private Long tenantId;
    @ApiModelProperty("租户显示名称")
    private String tenant;
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
