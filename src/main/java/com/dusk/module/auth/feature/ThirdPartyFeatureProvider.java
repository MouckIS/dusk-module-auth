package com.dusk.module.auth.feature;

import com.dusk.module.auth.impl.FeatureProvider;
import com.dusk.module.auth.service.IFeatureDefinitionContext;
import com.dusk.module.ddm.dto.ui.CheckBox;
import com.dusk.module.ddm.dto.ui.ComboBox;
import org.springframework.stereotype.Component;

/**
 * 第三方对接相关特性
 * @author kefuming
 * @date 2021-11-26 17:10
 */
@Component
public class ThirdPartyFeatureProvider extends FeatureProvider {
    public static final String APP_THIRD_PARTY = "App.Third.Party";
    public static final String APP_THIRD_PARTY_SINGLE_PAGE = "App.Third.Party.Single.Page";
    @Override
    public void setFeatures(IFeatureDefinitionContext context) {
        context.create(APP_THIRD_PARTY, "true", "第三方对接");
        context.createChildren(APP_THIRD_PARTY_SINGLE_PAGE, "false", "单页面显示", "",  new CheckBox());
    }
}
