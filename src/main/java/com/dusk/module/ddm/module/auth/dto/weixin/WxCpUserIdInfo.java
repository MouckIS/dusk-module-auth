package com.dusk.module.ddm.module.auth.dto.weixin;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author jianjianhong
 * @date 2023/10/12
 */
@NoArgsConstructor
@Data
public class WxCpUserIdInfo {

    private Integer errcode;
    private String errmsg;
    private String userid;
    private String user_ticket;
}
