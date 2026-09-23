package com.dusk.module.auth.service.impl;

import cn.hutool.extra.mail.MailAccount;
import com.dusk.common.rpc.auth.dto.setting.EmailShareLinkHostUrlOutput;
import com.dusk.module.auth.setting.provider.EmailSettingProvider;
import com.dusk.module.auth.setting.provider.HostSettingProvider;
import com.dusk.module.ddm.service.ISettingRpcService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * {@link EmailServiceImpl} 单元测试，目标：100% 分支覆盖。
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private ISettingRpcService settingRpcService;

    private EmailServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EmailServiceImpl();
        ReflectionTestUtils.setField(service, "settingRpcService", settingRpcService);
    }

    private void stubSmtpSettings() {
        when(settingRpcService.getValue(EmailSettingProvider.SMTP_HOST)).thenReturn("smtp.dusk.com");
        when(settingRpcService.getValue(EmailSettingProvider.SMTP_PORT)).thenReturn("465");
        when(settingRpcService.getValue(EmailSettingProvider.SMTP_USER_NAME)).thenReturn("mailer");
        when(settingRpcService.getValue(EmailSettingProvider.SMTP_PASSWORD)).thenReturn("secret");
        when(settingRpcService.getValue(EmailSettingProvider.SMTP_DEFAULT_FROM_ADDRESS)).thenReturn("noreply@dusk.com");
        when(settingRpcService.getValue(EmailSettingProvider.SMTP_ENABLE_SSL)).thenReturn("true");
        when(settingRpcService.getValue(EmailSettingProvider.SMTP_USE_DEFAULT_CREDENTIALS)).thenReturn("true");
    }

    @Test
    @DisplayName("getEmailShareLinkHostUrl：未配置域名时不回填 domain")
    void getShareLinkHostUrlWithoutDomain() {
        when(settingRpcService.getValue(HostSettingProvider.HOST_DOMAIN)).thenReturn("   ");
        when(settingRpcService.getValue(HostSettingProvider.HOST_SCHEMA)).thenReturn("true");

        EmailShareLinkHostUrlOutput output = service.getEmailShareLinkHostUrl();

        assertThat(output.getDomain()).isNull();
        assertThat(output.isHttpsEnabled()).isTrue();
    }

    @Test
    @DisplayName("getEmailShareLinkHostUrl：已配置域名时回填 domain")
    void getShareLinkHostUrlWithDomain() {
        when(settingRpcService.getValue(HostSettingProvider.HOST_DOMAIN)).thenReturn("dusk.com");
        when(settingRpcService.getValue(HostSettingProvider.HOST_SCHEMA)).thenReturn("false");

        EmailShareLinkHostUrlOutput output = service.getEmailShareLinkHostUrl();

        assertThat(output.getDomain()).isEqualTo("dusk.com");
        assertThat(output.isHttpsEnabled()).isFalse();
    }

    @Test
    @DisplayName("getTenantMailAccount：配置了显示名时拼接发件人")
    void getTenantMailAccountWithDisplayName() {
        stubSmtpSettings();
        when(settingRpcService.getValue(EmailSettingProvider.SMTP_DEFAULT_FROM_DISPLAY_NAME)).thenReturn("Dusk");

        MailAccount account = service.getTenantMailAccount();

        assertThat(account.getHost()).isEqualTo("smtp.dusk.com");
        assertThat(account.getPort()).isEqualTo(465);
        assertThat(account.getFrom()).isEqualTo("Dusk <noreply@dusk.com>");
        assertThat(account.getUser()).isEqualTo("mailer");
        assertThat(account.getPass()).isEqualTo("secret");
    }

    @Test
    @DisplayName("getTenantMailAccount：未配置显示名时直接使用发件地址")
    void getTenantMailAccountWithoutDisplayName() {
        stubSmtpSettings();
        when(settingRpcService.getValue(EmailSettingProvider.SMTP_DEFAULT_FROM_DISPLAY_NAME)).thenReturn("");

        MailAccount account = service.getTenantMailAccount();

        assertThat(account.getFrom()).isEqualTo("noreply@dusk.com");
    }

    @Test
    @DisplayName("getTenantMailAccount：端口号非数字时抛出业务异常")
    void getTenantMailAccountRejectsInvalidPort() {
        when(settingRpcService.getValue(EmailSettingProvider.SMTP_HOST)).thenReturn("smtp.dusk.com");
        when(settingRpcService.getValue(EmailSettingProvider.SMTP_PORT)).thenReturn("not-a-number");
        when(settingRpcService.getValue(EmailSettingProvider.SMTP_USER_NAME)).thenReturn("mailer");
        when(settingRpcService.getValue(EmailSettingProvider.SMTP_PASSWORD)).thenReturn("secret");

        assertThatThrownBy(() -> service.getTenantMailAccount())
                .isInstanceOf(com.dusk.common.core.exception.BusinessException.class);
    }
}
