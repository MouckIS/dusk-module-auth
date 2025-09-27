package com.dusk.module.ddm.module.auth.setting.provider;

import com.dusk.common.core.feature.ui.CheckBox;
import com.dusk.common.core.feature.ui.SingerLineString;
import com.dusk.common.core.setting.SettingDefinition;
import com.dusk.common.core.setting.SettingProvider;
import com.dusk.common.core.setting.SettingScopes;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 人员管控配置
 *
 * @author kefuming
 * @CreateTime 2022-10-28
 */
@Component
public class PersonnelControlSettingProvider extends SettingProvider {
    public static final String PERSONNEL_CONTROL_SETTINGS = "Personnel.Control.Settings";
    public static final String PERSONNEL_CONTROL_PASSWORD = "Personnel.Control.Settings.password";
    public static final String PERSONNEL_CONTROL_CREATE_EXTERNAL_USER = "Personnel.Control.Create.ExternalUser";
    public static final String PERSONNEL_CONTROL_CONFIRM_PHONE = "Personnel.Control.Confirm.phone";
    public static final String PERSONNEL_CONTROL_CONFIRM_ID_CARD = "Personnel.Control.Confirm.idCard";

    @Override
    public List<SettingDefinition> getSettingDefinitions() {
        return new ArrayList<>() {{
            add(new SettingDefinition.Builder(PERSONNEL_CONTROL_SETTINGS, "").displayName("人员管控配置")
                    .scopes(SettingScopes.Application, SettingScopes.Tenant).inputType(null).build());
            add(new SettingDefinition.Builder(PERSONNEL_CONTROL_PASSWORD, "123456").displayName("默认密码")
                    .scopes(SettingScopes.Application, SettingScopes.Tenant).parent(PERSONNEL_CONTROL_SETTINGS).inputType(new SingerLineString()).build());
            add(new SettingDefinition.Builder(PERSONNEL_CONTROL_CREATE_EXTERNAL_USER, "").displayName("创建外单位用户配置")
                    .scopes(SettingScopes.Application, SettingScopes.Tenant).parent(PERSONNEL_CONTROL_SETTINGS).inputType(null).build());
            add(new SettingDefinition.Builder(PERSONNEL_CONTROL_CONFIRM_PHONE, "true").displayName("手机号码是否必填")
                    .scopes(SettingScopes.Application, SettingScopes.Tenant).parent(PERSONNEL_CONTROL_CREATE_EXTERNAL_USER).inputType(new CheckBox()).build());
            add(new SettingDefinition.Builder(PERSONNEL_CONTROL_CONFIRM_ID_CARD, "true").displayName("身份证号码是否必填")
                    .scopes(SettingScopes.Application, SettingScopes.Tenant).parent(PERSONNEL_CONTROL_CREATE_EXTERNAL_USER).inputType(new CheckBox()).build());
        }};
    }
}
