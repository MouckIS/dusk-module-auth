package com.dusk.module.auth.dto.weixin;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author jianjianhong
 * @date 2023/10/12
 */
@NoArgsConstructor
@Getter
@Setter
public class WxCpAccessTokenInfo {
    private Integer errcode;
    private String errmsg;
    private String access_token;
    private Integer expires_in;
}
