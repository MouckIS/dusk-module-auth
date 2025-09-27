package com.dusk.module.auth.push.local.dto;

import lombok.Getter;
import lombok.Setter;
import com.dusk.common.core.pusher.Navigation;
import com.dusk.common.core.pusher.NoticationLevel;

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
