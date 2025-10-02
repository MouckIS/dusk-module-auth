package com.dusk.module.auth.push.local.dto;

import com.dusk.common.mqs.enums.NoticationLevel;
import com.dusk.common.mqs.pusher.Navigation;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2022/11/24 8:00
 */
@Getter
@Setter
public class NotificationMsg {
    /**
     * 消息唯一ID
     */
    private String messageId;

    private Navigation navigation;

    private NoticationLevel level;

    private NotificationContent content;
}
