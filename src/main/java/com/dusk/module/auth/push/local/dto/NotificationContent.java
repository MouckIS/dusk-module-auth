package com.dusk.module.auth.push.local.dto;

import cn.hutool.core.date.DateTime;
import lombok.Getter;
import lombok.Setter;
import com.dusk.common.core.pusher.PushDeviceType;
import com.dusk.common.core.pusher.PushTarget;

/**
 * @author kefuming
 * @date 2022/11/24 8:15
 */
@Getter
@Setter
public class NotificationContent {
    /**
     * 推送目标: DEVICE:按设备推送 ALIAS : 按别名推送 ACCOUNT:按帐号推送  TAG:按标签推送; ALL: 广播推送
     */
    private PushTarget target;

    /**
     * 根据Target来设定，如Target=DEVICE, 则对应的值为 设备id1,设备id2. 多个值使用逗号分隔.(帐号与设备有一次最多100个的限制)
     */
    private String targetValue;

    /**
     * 设备类型 ANDROID iOS ALL.
     */
    private PushDeviceType deviceType;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String body;

    /**
     * 时间格式：2022-11-24T05:08:04.696Z
     * 推送控制
     * 可以设置成你指定固定时间 默认最少3秒
     */
    private String pushTime;

    /**
     * 最少3秒,默认72小时。离线消息的过期时间，过期则不会再被发送。离线消息最长保存72小时，过期时间时长不会超过发送时间加72小时。
     */
    private String expireTime;

    /**
     * 离线消息是否保存,若保存, 在推送时候，用户即使不在线，下一次上线则会收到
     */
    private boolean storeOffline;
}
