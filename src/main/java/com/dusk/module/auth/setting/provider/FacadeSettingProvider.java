package com.dusk.module.auth.setting.provider;

import com.dusk.common.framework.feature.ui.FileInput;
import com.dusk.common.framework.setting.SettingDefinition;
import com.dusk.common.framework.setting.SettingProvider;
import com.dusk.common.framework.setting.SettingScopes;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author kefuming
 * @date 2020-05-20 15:31
 */
@Component
public class FacadeSettingProvider extends SettingProvider {
    public static final String FACADE_SETTINGS = "Facade.Settings";

    public static final String CSS = "Facade.Settings.CSS";

    //region 登录页
    /**
     * web登录页左上角logo
     */
    public static final String WEB_LOGIN_PAGE_LOGO = "Facade.Settings.Web.Login.Page.Logo";
    /**
     * web登录页背景图
     */
    public static final String WEB_LOGIN_PAGE_BACKGROUND = "Facade.Settings.Web.Login.Page.Background";
    //endregion

    //region 主页
    /**
     * web主页左上角logo（大）
     */
    public static final String WEB_INDEX_PAGE_LOGO_LARGE = "Facade.Settings.Web.Index.Page.Logo.Large";
    /**
     * web主页左上角logo（小）
     */
    public static final String WEB_INDEX_PAGE_LOGO_MINI = "Facade.Settings.Web.Index.Page.Logo.Mini";

    /**
     * Favicon图标
     */
    public static final String WEB_FAVICON = "Facade.Settings.Web.Favicon";
    //endregion

    @Override
    public List<SettingDefinition> getSettingDefinitions() {
        return new ArrayList<>() {{
            add(new SettingDefinition.Builder(FACADE_SETTINGS, "").displayName("外观").scopes(SettingScopes.Application, SettingScopes.Tenant).inputType(null).build());
            add(new SettingDefinition.Builder(CSS, "").displayName("CSS样式表").description("选择后缀名为css的文件").scopes(SettingScopes.Application, SettingScopes.Tenant)
                    .inputType(new FileInput()).parent(FACADE_SETTINGS).build());
            add(new SettingDefinition.Builder(WEB_LOGIN_PAGE_LOGO, "").displayName("web登录页左上角logo").description("选择一张大小为138*44px大小的图片")
                    .scopes(SettingScopes.Application, SettingScopes.Tenant).inputType(new FileInput()).parent(FACADE_SETTINGS).build());
            add(new SettingDefinition.Builder(WEB_LOGIN_PAGE_BACKGROUND, "").displayName("web登录页背景图").description("选择一张大小为1920*1080px大小的图片")
                    .scopes(SettingScopes.Application, SettingScopes.Tenant).inputType(new FileInput()).parent(FACADE_SETTINGS).build());
            add(new SettingDefinition.Builder(WEB_INDEX_PAGE_LOGO_LARGE, "").displayName("web主页左上角logo（大）").description("选择一张大小为138*44px大小的图片")
                    .scopes(SettingScopes.Application, SettingScopes.Tenant).inputType(new FileInput()).parent(FACADE_SETTINGS).build());
            add(new SettingDefinition.Builder(WEB_INDEX_PAGE_LOGO_MINI, "").displayName("web主页左上角logo（小）").description("选择一张大小为40*44px大小的图片")
                    .scopes(SettingScopes.Application, SettingScopes.Tenant).inputType(new FileInput()).parent(FACADE_SETTINGS).build());
            add(new SettingDefinition.Builder(WEB_FAVICON, "").displayName("Favicon图标").description("选择一张大小为32*32px大小的图片")
                    .scopes(SettingScopes.Application, SettingScopes.Tenant).inputType(new FileInput()).parent(FACADE_SETTINGS).build());
        }};
    }
}
