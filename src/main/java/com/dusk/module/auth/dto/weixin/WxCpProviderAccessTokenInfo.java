package com.dusk.module.auth.dto.weixin;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * @author jianjianhong
 * @date 2023/11/12
 */
@NoArgsConstructor
@Getter
@Setter
public class WxCpProviderAccessTokenInfo {

    private String provider_access_token;
    private Integer expires_in;
}
