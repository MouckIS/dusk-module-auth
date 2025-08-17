package com.dusk.module.auth.setting;

import com.dusk.common.framework.setting.ISettingClientVisibilityProvider;

/**
 * @author kefuming
 * @date 2020-05-20 11:28
 */
public class VisibleSettingClientVisibilityProvider implements ISettingClientVisibilityProvider {
    private static VisibleSettingClientVisibilityProvider INSTANCE;

    private VisibleSettingClientVisibilityProvider(){}

    public static VisibleSettingClientVisibilityProvider getInstance(){
        if(INSTANCE == null){
            INSTANCE = new VisibleSettingClientVisibilityProvider();
        }
        return INSTANCE;
    }

    @Override
    public boolean checkVisible() {
        return true;
    }
}
