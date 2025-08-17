package com.dusk.module.auth.setting.provider;

import com.dusk.common.framework.feature.ui.CheckBox;
import com.dusk.common.framework.setting.SettingDefinition;
import com.dusk.common.framework.setting.SettingProvider;
import com.dusk.common.framework.setting.SettingScopes;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: pengmengjiang
 * @Date: 2021/2/4 15:32
 */
@Component
public class EmailSettingProvider extends SettingProvider {
    public static final String MAIL_SMTP = "Mail.Smtp";
    public static final String SMTP_HOST = "Mail.Smtp.Host";
    public static final String SMTP_PORT = "Mail.Smtp.Port";
    public static final String SMTP_USER_NAME = "Mail.Smtp.UserName";
    public static final String SMTP_PASSWORD = "Mail.Smtp.Password";
    public static final String SMTP_ENABLE_SSL = "Mail.Smtp.EnableSsl";
    public static final String SMTP_USE_DEFAULT_CREDENTIALS = "Mail.Smtp.UseDefaultCredentials";
    public static final String SMTP_DEFAULT_FROM_ADDRESS = "Mail.Smtp.DefaultFromAddress";
    public static final String SMTP_DEFAULT_FROM_DISPLAY_NAME = "Mail.Smtp.DefaultFromDisplayName";

    @Override
    public List<SettingDefinition> getSettingDefinitions() {
        return new ArrayList<>(){{
            add(new SettingDefinition.Builder(MAIL_SMTP, "").displayName("SMTP邮箱配置").scopes(SettingScopes.Application, SettingScopes.Tenant).inputType(null).build());
            add(new SettingDefinition.Builder(SMTP_HOST,"").displayName("SMTP服务器").scopes(SettingScopes.Application, SettingScopes.Tenant).parent(MAIL_SMTP).build());
            add(new SettingDefinition.Builder(SMTP_PORT,"25").displayName("SMTP端口").scopes(SettingScopes.Application, SettingScopes.Tenant).parent(MAIL_SMTP).build());
            add(new SettingDefinition.Builder(SMTP_USER_NAME,"").displayName("用户名").scopes(SettingScopes.Application, SettingScopes.Tenant).parent(MAIL_SMTP).build());
            add(new SettingDefinition.Builder(SMTP_PASSWORD,"").displayName("密码").scopes(SettingScopes.Application, SettingScopes.Tenant).parent(MAIL_SMTP).build());
            add(new SettingDefinition.Builder(SMTP_ENABLE_SSL,"false").displayName("使用SSL").scopes(SettingScopes.Application, SettingScopes.Tenant).inputType(new CheckBox()).parent(MAIL_SMTP).build());
            add(new SettingDefinition.Builder(SMTP_USE_DEFAULT_CREDENTIALS,"true").displayName("默认身份验证").scopes(SettingScopes.Application, SettingScopes.Tenant).inputType(new CheckBox()).parent(MAIL_SMTP).build());
            add(new SettingDefinition.Builder(SMTP_DEFAULT_FROM_ADDRESS,"").displayName("发送邮箱地址").scopes(SettingScopes.Application, SettingScopes.Tenant).parent(MAIL_SMTP).build());
            add(new SettingDefinition.Builder(SMTP_DEFAULT_FROM_DISPLAY_NAME,"").displayName("发送人名字").scopes(SettingScopes.Application, SettingScopes.Tenant).parent(MAIL_SMTP).build());
        }};
    }
}
