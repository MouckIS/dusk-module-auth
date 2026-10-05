package com.dusk.module.auth.dto.weixin;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author jianjianhong
 * @date 2023/11/22
 */
@NoArgsConstructor
@Getter
@Setter
public class WxCpUserDetail {

    private Integer errcode;
    private String errmsg;
    private String userid;
    private String gender;
    private String avatar;
    private String qr_code;
    private String mobile;
    private String email;
    private String biz_mail;
    private String address;
}
