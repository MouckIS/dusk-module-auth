package com.dusk.module.auth.push;

import cn.hutool.json.JSONUtil;
import com.dusk.common.mqs.enums.PushTarget;
import com.dusk.common.mqs.enums.PushType;
import com.dusk.common.mqs.pusher.Navigation;
import com.dusk.common.mqs.pusher.NotificationOption;
import com.dusk.common.mqs.pusher.PushMessage;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * 消息推送的内容
 *
 * @author kefuming
 * @date 2020/9/21 15:35
 */
public class Payload {

    /**
     * 额外参数
     */
    @Getter
    private final String exParams;

    /**
     * 安卓客户端额外参数
     */
    @Getter
    @JsonProperty(value = "AndroidExtParameters")
    private final String androidExtParameters;

    /**
     * 小米手机的推送标题
     */
    @Getter
    @JsonProperty(value = "AndroidXiaoMiNotifyTitle")
    private final String androidXiaoMiNotifyTitle;

    /**
     * 小米手机的推送内容
     */
    @Getter
    @JsonProperty(value = "AndroidXiaoMiNotifyBody")
    private final String androidXiaoMiNotifyBody;

    /**
     * IOS客户端额外参数
     */
    @Getter
    @JsonProperty(value = "iOSExtParameters")
    private final String iOSExtParameters;

    public Payload(
            PushType pushType,
            PushMessage pushMessage,
            NotificationOption options,
            Navigation navigation) {
        this.pushType = pushType.toString();
        this.deviceType = pushMessage.getDeviceType().toString();
        this.title = pushMessage.getTitle();
        this.body = pushMessage.getBody();
        this.expireTime = pushMessage.getExpireTime().toString("yyyy-MM-dd'T'hh:mm:ss'Z'");
        this.pushTime = pushMessage.getPushTime().toString("yyyy-MM-dd'T'hh:mm:ss'Z'");
        this.target = pushMessage.getTarget().toString();
        this.targetValue = pushMessage.getTarget().equals(PushTarget.ALL) ? "ALL" : pushMessage.getTargetValue();
        this.storeOffline = pushMessage.isStoreOffline();
        Map<String, Object> extParamsMap = new HashMap<>();
        if (navigation != null) {
            extParamsMap.put("navigation", JSONUtil.toJsonStr(navigation));
        }
        extParamsMap.put("options", options);
        this.exParams = JSONUtil.toJsonStr(extParamsMap);
        androidExtParameters = exParams;
        iOSExtParameters = exParams;
        androidXiaoMiNotifyTitle = title;
        androidXiaoMiNotifyBody = body;
    }

    /**
     * App Key
     */
    @Getter
    @Setter
    @JsonProperty(value = "AppKey")
    private String appKey;

    /**
     * 推送目标: DEVICE:按设备推送 ALIAS : 按别名推送 ACCOUNT:按帐号推送  TAG:按标签推送; ALL: 广播推送
     */
    @Getter
    @Setter
    @JsonProperty(value = "Target")
    private String target;

    /**
     * 根据Target来设定，如Target=DEVICE, 则对应的值为 设备id1,设备id2. 多个值使用逗号分隔.(帐号与设备有一次最多100个的限制)
     */
    @Getter
    @Setter
    @JsonProperty(value = "TargetValue")
    private String targetValue;

    /**
     * 消息类型 MESSAGE NOTICE
     */
    @Getter
    @Setter
    @JsonProperty(value = "PushType")
    private String pushType;

    /**
     * 设备类型 ANDROID iOS ALL.
     */
    @Getter
    @Setter
    @JsonProperty(value = "DeviceType")
    private String deviceType;

    /**
     * 标题
     */
    @Getter
    @Setter
    @JsonProperty(value = "Title")
    private String title;

    /**
     * 内容
     */
    @Getter
    @Setter
    @JsonProperty(value = "Body")
    private String body;

    /**
     * 推送控制
     * 可以设置成你指定固定时间
     */
    @Getter
    @Setter
    @JsonProperty(value = "PushTime")
    private String pushTime;

    /**
     * 离线消息是否保存,若保存, 在推送时候，用户即使不在线，下一次上线则会收到
     */
    @Getter
    @Setter
    @JsonProperty(value = "StoreOffline")
    private boolean storeOffline;

    /**
     * 离线消息的过期时间，过期则不会再被发送。离线消息最长保存72小时，过期时间时长不会超过发送时间加72小时。
     */
    @Getter
    @Setter
    @JsonProperty(value = "ExpireTime")
    private String expireTime;

}