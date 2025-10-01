package com.dusk.module.auth.feature;

import com.dusk.module.auth.dto.TenantFeature;
import com.dusk.module.auth.impl.FeatureProvider;
import com.dusk.module.auth.service.IFeatureDefinitionContext;
import com.dusk.module.ddm.dto.ui.CheckBox;
import com.dusk.module.ddm.dto.ui.SingerLineString;
import org.springframework.stereotype.Component;

/**
 * app顶部消息推送特性
 * @author kefuming
 * @date 2022/11/26 13:06
 */
@Component
public class AppNotificationProvider extends FeatureProvider {

    public static final String APP_NOTIFICATION_LOCAL_ENABLED = "App.Notification.Local.Enabled";
    public static final String APP_NOTIFICATION_LOCAL_HOST = "App.Notification.Local.Host";


    @Override
    public void setFeatures(IFeatureDefinitionContext context) {
        TenantFeature notificationLocal = context.create(APP_NOTIFICATION_LOCAL_ENABLED, "false", "APP顶部消息推送就地模式", "", new CheckBox());
        context.createChildren(notificationLocal, APP_NOTIFICATION_LOCAL_HOST, "", "消息发布地址", "", new SingerLineString());
    }
}