package com.dusk.module.auth.dto.weixin;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author jianjianhong
 * @date 2023/11/12
 */
@NoArgsConstructor
@Data
public class WxOpenCorpIdInfo {

    private Integer errcode;
    private String errmsg;
    private String open_corpid;
}
