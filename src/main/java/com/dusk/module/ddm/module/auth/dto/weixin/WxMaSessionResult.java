package com.dusk.module.ddm.module.auth.dto.weixin;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.dusk.module.auth.dto.mobilelogin.MobileUserDto;

import java.util.List;

/**
 * @author kefuming
 * @date 2020-12-25 11:18
 */
@Getter
@Setter
public class WxMaSessionResult {

    private String openid;

    private String unionid;

    @ApiModelProperty("登陆token列表")
    private List<MobileUserDto> loginData;
}
