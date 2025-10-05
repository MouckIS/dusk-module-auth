package com.dusk.module.auth.dto.weixin;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * @author jianjianhong
 * @date 2023/10/12
 */
@AllArgsConstructor
@Getter
@Setter
public class WxCpUserAuthorizationResult {

    private String wxUserId;
    private String token;
}
