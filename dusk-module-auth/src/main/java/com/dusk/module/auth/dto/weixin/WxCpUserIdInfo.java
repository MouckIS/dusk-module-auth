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
public class WxCpUserIdInfo {

    private Integer errcode;
    private String errmsg;
    private String userid;
    private String user_ticket;
}
