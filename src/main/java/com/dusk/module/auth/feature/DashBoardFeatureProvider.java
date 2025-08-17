
package com.dusk.module.auth.feature;

import com.dusk.common.framework.feature.IFeatureDefinitionContext;
import com.dusk.common.framework.feature.impl.FeatureProvider;
import com.dusk.common.framework.feature.ui.CheckBox;
import org.springframework.stereotype.Component;

/**
 *
 * 〈数据大屏相关特性〉
 *
 * @author jianjianhong
 * @create 2021/09/14
 * @since 1.0.0
 */
@Component
public class DashBoardFeatureProvider extends FeatureProvider {

    public static final String APP_DASHBOARD = "App.DashBoard";


    @Override
    public void setFeatures(IFeatureDefinitionContext context) {
        context.create(APP_DASHBOARD, "false", "数据大屏特性", "", new CheckBox());
    }
}
