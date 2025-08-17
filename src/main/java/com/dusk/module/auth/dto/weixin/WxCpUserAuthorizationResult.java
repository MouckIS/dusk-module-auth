package com.dusk.module.auth.dto.weixin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author jianjianhong
 * @date 2023/10/12
 */
@AllArgsConstructor
@Data
public class WxCpUserAuthorizationResult {

    private String wxUserId;
    private String token;
}
