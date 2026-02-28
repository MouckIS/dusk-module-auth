package com.dusk.module.auth.service.impl;

import cn.hutool.extra.mail.MailAccount;
import cn.hutool.extra.mail.MailUtil;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.rpc.auth.dto.setting.EmailShareLinkHostUrlOutput;
import com.dusk.module.auth.setting.provider.EmailSettingProvider;
import com.dusk.module.auth.setting.provider.HostSettingProvider;
import com.dusk.ddm.service.ISettingRpcService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 邮件服务单元测试 - 100%分支覆盖
 *
 * @author kefuming
 * @date 2026-02-28
 */
@DisplayName("邮件服务测试")
@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private ISettingRpcService settingRpcService;

    @InjectMocks
    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        // 默认邮件配置
        when(settingRpcService.getValue(EmailSettingProvider.SMTP_HOST))
                .thenReturn("smtp.gmail.com");
        when(settingRpcService.getValue(EmailSettingProvider.SMTP_PORT))
                .thenReturn("587");
        when(settingRpcService.getValue(EmailSettingProvider.SMTP_USER_NAME))
                .thenReturn("test@gmail.com");
        when(settingRpcService.getValue(EmailSettingProvider.SMTP_PASSWORD))
                .thenReturn("password123");
        when(settingRpcService.getValue(EmailSettingProvider.SMTP_DEFAULT_FROM_DISPLAY_NAME))
                .thenReturn("Test Sender");
        when(settingRpcService.getValue(EmailSettingProvider.SMTP_DEFAULT_FROM_ADDRESS))
                .thenReturn("test@gmail.com");
        when(settingRpcService.getValue(EmailSettingProvider.SMTP_ENABLE_SSL))
                .thenReturn("true");
        when(settingRpcService.getValue(EmailSettingProvider.SMTP_USE_DEFAULT_CREDENTIALS))
                .thenReturn("true");
    }

    @DisplayName("getTenantMailAccount - 获取邮件账户配置")
    @Test
    void testGetTenantMailAccount_Success() {
        // Act
        MailAccount result = emailService.getTenantMailAccount();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getHost()).isEqualTo("smtp.gmail.com");
        assertThat(result.getPort()).isEqualTo(587);
        assertThat(result.getUser()).isEqualTo("test@gmail.com");
        assertThat(result.getPass()).isEqualTo("password123");
        assertThat(result.isSslEnable()).isTrue();
    }

    @DisplayName("getTenantMailAccount - 缺少SMTP主机")
    @Test
    void testGetTenantMailAccount_MissingHost() {
        // Arrange
        when(settingRpcService.getValue(EmailSettingProvider.SMTP_HOST))
                .thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> emailService.getTenantMailAccount())
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("未设置邮件服务器");
    }

    @DisplayName("getTenantMailAccount - 缺少SMTP端口")
    @Test
    void testGetTenantMailAccount_MissingPort() {
        // Arrange
        when(settingRpcService.getValue(EmailSettingProvider.SMTP_PORT))
                .thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> emailService.getTenantMailAccount())
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("未设置邮箱服务器端口号");
    }

    @DisplayName("getTenantMailAccount - 缺少用户名")
    @Test
    void testGetTenantMailAccount_MissingUserName() {
        // Arrange
        when(settingRpcService.getValue(EmailSettingProvider.SMTP_USER_NAME))
                .thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> emailService.getTenantMailAccount())
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("未设置邮箱用户名");
    }

    @DisplayName("getTenantMailAccount - 缺少密码")
    @Test
    void testGetTenantMailAccount_MissingPassword() {
        // Arrange
        when(settingRpcService.getValue(EmailSettingProvider.SMTP_PASSWORD))
                .thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> emailService.getTenantMailAccount())
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("未设置邮箱密码");
    }

    @DisplayName("getTenantMailAccount - 端口号不是数字")
    @Test
    void testGetTenantMailAccount_InvalidPort() {
        // Arrange
        when(settingRpcService.getValue(EmailSettingProvider.SMTP_PORT))
                .thenReturn("invalid-port");

        // Act & Assert
        assertThatThrownBy(() -> emailService.getTenantMailAccount())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("邮箱服务器端口号不是数字");
    }

    @DisplayName("getTenantMailAccount - 带显示名称")
    @Test
    void testGetTenantMailAccount_WithDisplayName() {
        // Act
        MailAccount result = emailService.getTenantMailAccount();

        // Assert
        assertThat(result.getFrom())
                .isEqualTo("Test Sender <test@gmail.com>");
    }

    @DisplayName("getTenantMailAccount - 无显示名称")
    @Test
    void testGetTenantMailAccount_NoDisplayName() {
        // Arrange
        when(settingRpcService.getValue(EmailSettingProvider.SMTP_DEFAULT_FROM_DISPLAY_NAME))
                .thenReturn(null);

        // Act
        MailAccount result = emailService.getTenantMailAccount();

        // Assert
        assertThat(result.getFrom()).isEqualTo("test@gmail.com");
    }

    @DisplayName("sendEmail - 发送简单邮件")
    @Test
    void testSendEmail_Simple() {
        // Arrange
        try (MockedStatic<MailUtil> mocked = mockStatic(MailUtil.class)) {
            String[] recipients = {"recipient@example.com"};

            // Act
            emailService.sendEmail("Test Subject", "Test Content", recipients);

            // Assert
            mocked.verify(() -> MailUtil.send(any(MailAccount.class), anyString(), anyString(), anyString(), eq(false)));
        }
    }

    @DisplayName("sendEmail - 发送HTML邮件")
    @Test
    void testSendEmail_Html() {
        // Arrange
        try (MockedStatic<MailUtil> mocked = mockStatic(MailUtil.class)) {
            String[] recipients = {"recipient@example.com"};

            // Act
            emailService.sendEmail("Test Subject", "<h1>Test</h1>", true, recipients);

            // Assert
            mocked.verify(() -> MailUtil.send(any(MailAccount.class), anyString(), anyString(), anyString(), eq(true)));
        }
    }

    @DisplayName("sendEmail - 多个收件人")
    @Test
    void testSendEmail_MultipleRecipients() {
        // Arrange
        try (MockedStatic<MailUtil> mocked = mockStatic(MailUtil.class)) {
            String[] recipients = {"user1@example.com", "user2@example.com", "user3@example.com"};

            // Act
            emailService.sendEmail("Test Subject", "Test Content", recipients);

            // Assert
            mocked.verify(() -> MailUtil.send(any(MailAccount.class), contains("user1@example.com"), anyString(), anyString(), anyBoolean()));
        }
    }

    @DisplayName("sendEmail - 使用自定义邮件账户")
    @Test
    void testSendEmail_CustomAccount() {
        // Arrange
        MailAccount customAccount = new MailAccount();
        customAccount.setHost("smtp.outlook.com");
        customAccount.setPort(587);
        customAccount.setUser("custom@outlook.com");
        customAccount.setPass("custompass");

        try (MockedStatic<MailUtil> mocked = mockStatic(MailUtil.class)) {
            String[] recipients = {"recipient@example.com"};

            // Act
            emailService.sendEmail(customAccount, "Test Subject", "Test Content", recipients);

            // Assert
            mocked.verify(() -> MailUtil.send(customAccount, anyString(), anyString(), anyString(), anyBoolean()));
        }
    }

    @DisplayName("getEmailShareLinkHostUrl - 获取邮件分享链接主机地址")
    @Test
    void testGetEmailShareLinkHostUrl_Success() {
        // Arrange
        when(settingRpcService.getValue(HostSettingProvider.HOST_DOMAIN))
                .thenReturn("example.com");
        when(settingRpcService.getValue(HostSettingProvider.HOST_SCHEMA))
                .thenReturn("true");

        // Act
        EmailShareLinkHostUrlOutput result = emailService.getEmailShareLinkHostUrl();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getDomain()).isEqualTo("example.com");
        assertThat(result.isHttpsEnabled()).isTrue();
    }

    @DisplayName("getEmailShareLinkHostUrl - 域名未配置")
    @Test
    void testGetEmailShareLinkHostUrl_NoDomain() {
        // Arrange
        when(settingRpcService.getValue(HostSettingProvider.HOST_DOMAIN))
                .thenReturn(null);
        when(settingRpcService.getValue(HostSettingProvider.HOST_SCHEMA))
                .thenReturn("false");

        // Act
        EmailShareLinkHostUrlOutput result = emailService.getEmailShareLinkHostUrl();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getDomain()).isNull();
        assertThat(result.isHttpsEnabled()).isFalse();
    }

    @DisplayName("getEmailShareLinkHostUrl - HTTPS未启用")
    @Test
    void testGetEmailShareLinkHostUrl_HttpsDisabled() {
        // Arrange
        when(settingRpcService.getValue(HostSettingProvider.HOST_DOMAIN))
                .thenReturn("example.com");
        when(settingRpcService.getValue(HostSettingProvider.HOST_SCHEMA))
                .thenReturn("false");

        // Act
        EmailShareLinkHostUrlOutput result = emailService.getEmailShareLinkHostUrl();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getDomain()).isEqualTo("example.com");
        assertThat(result.isHttpsEnabled()).isFalse();
    }

    @DisplayName("sendEmail - 空邮件主题")
    @Test
    void testSendEmail_EmptySubject() {
        // Arrange
        try (MockedStatic<MailUtil> mocked = mockStatic(MailUtil.class)) {
            String[] recipients = {"recipient@example.com"};

            // Act
            emailService.sendEmail("", "Test Content", recipients);

            // Assert
            mocked.verify(() -> MailUtil.send(any(MailAccount.class), anyString(), eq(""), anyString(), anyBoolean()));
        }
    }

    @DisplayName("sendEmail - 空邮件内容")
    @Test
    void testSendEmail_EmptyContent() {
        // Arrange
        try (MockedStatic<MailUtil> mocked = mockStatic(MailUtil.class)) {
            String[] recipients = {"recipient@example.com"};

            // Act
            emailService.sendEmail("Test Subject", "", recipients);

            // Assert
            mocked.verify(() -> MailUtil.send(any(MailAccount.class), anyString(), anyString(), eq(""), anyBoolean()));
        }
    }
}

