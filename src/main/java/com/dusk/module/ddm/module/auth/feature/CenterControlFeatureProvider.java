package com.dusk.module.ddm.module.auth.feature;

import com.dusk.common.core.feature.IFeatureDefinitionContext;
import com.dusk.common.core.feature.impl.FeatureProvider;
import com.dusk.common.core.feature.ui.CheckBox;
import org.springframework.stereotype.Component;

/**
 * 集控模式相关特性 主要是处理厂站id  目前支持柜子业务
 *
 * @author kefuming
 * @date 2021-09-28 9:16
 */
@Component
public class CenterControlFeatureProvider extends FeatureProvider {

    public static final String STATION_DOWNWARD = "App.Station.Downward";
    public static final String STATION_CENTER_CONTROL = "App.Station.CenterControl";
    public static final String STATION_MANAGE = "App.Station";

    @Override
    public void setFeatures(IFeatureDefinitionContext context) {
        context.create(STATION_MANAGE, "true", "厂站管理");
        context.createChildren(STATION_CENTER_CONTROL, "false", "集控模式（柜子业务）", "", new CheckBox());
        context.createChildren(STATION_DOWNWARD, "false", "禁止厂站权限向下传递（用电侧业务）", "", new CheckBox());
    }
}
