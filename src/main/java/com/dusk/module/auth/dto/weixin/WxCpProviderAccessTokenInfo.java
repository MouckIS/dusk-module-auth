package com.dusk.module.auth.dto.weixin;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author jianjianhong
 * @date 2023/11/12
 */
@NoArgsConstructor
@Data
public class WxCpProviderAccessTokenInfo {

    private String provider_access_token;
    private Integer expires_in;
}
