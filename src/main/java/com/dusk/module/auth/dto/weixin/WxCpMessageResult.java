package com.dusk.module.auth.dto.weixin;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * @author jianjianhong
 * @date 2023/10/12
 */
@NoArgsConstructor
@Getter
@Setter
public class WxCpMessageResult {
    private Integer errcode;
    private String errmsg;
    private String invaliduser;
    private String invalidparty;
    private String invalidtag;
    private String unlicenseduser;
    private String msgid;
    private String response_code;
}
