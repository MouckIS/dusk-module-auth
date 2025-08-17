package com.dusk.module.auth.dto.weixin;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @Author zhuokangjun
 * @Date 2023/11/09 15:15
 */
@Getter
@Setter
public class WxTextDTO implements Serializable {
    private String content;
}
