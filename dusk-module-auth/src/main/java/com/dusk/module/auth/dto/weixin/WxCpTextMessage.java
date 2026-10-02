package com.dusk.module.auth.dto.weixin;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * @author zhuokangjun
 * @date 2023/11/09 15:16
 */
@Getter
@Setter
public class WxCpTextMessage implements Serializable {
    private List<Long> userId;
    private String touser;
    private String toparty;
    private String totag;
    private String msgtype;
    private Integer agentid;
    private WxTextDTO text;
}
