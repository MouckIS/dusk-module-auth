package com.dusk.module.auth.dto.weixin;

import com.dusk.module.auth.dto.mobilelogin.MobileUserDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

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

    @Schema(description = "登陆token列表")
    private List<MobileUserDto> loginData;
}
