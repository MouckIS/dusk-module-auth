
package com.dusk.module.auth.feature;

import com.dusk.module.auth.impl.FeatureProvider;
import com.dusk.module.auth.service.IFeatureDefinitionContext;
import com.dusk.module.ddm.dto.ui.CheckBox;
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
