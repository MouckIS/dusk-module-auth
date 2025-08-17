
package com.dusk.module.auth.feature;

import com.dusk.common.framework.feature.IFeatureDefinitionContext;
import com.dusk.common.framework.feature.impl.FeatureProvider;
import com.dusk.common.framework.feature.ui.CheckBox;
import org.springframework.stereotype.Component;

/**
 *
 * 〈待办相关特性〉
 *
 * @author kefuming
 * @create 2021/4/23
 * @since 1.0.0
 */
@Component
public class TodoFeatureProvider extends FeatureProvider {

    public static final String APP_TODO = "App.Todo";


    @Override
    public void setFeatures(IFeatureDefinitionContext context) {
        context.create(APP_TODO, "true", "APP待办特性", "", new CheckBox());
    }
}
