package com.dusk.module.auth.dto.weixin;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author jianjianhong
 * @date 2023/11/12
 */
@NoArgsConstructor
@Getter
@Setter
public class WxOpenCorpIdInfo {

    private Integer errcode;
    private String errmsg;
    private String open_corpid;
}
