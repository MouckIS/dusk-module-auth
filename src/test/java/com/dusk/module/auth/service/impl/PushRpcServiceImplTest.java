package com.dusk.module.auth.service.impl;

import com.dusk.common.core.exception.BusinessException;
import com.dusk.module.auth.push.INotificationPushManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

/**
 * {@link PushRpcServiceImpl} 单元测试，目标：100% 分支覆盖。
 */
@ExtendWith(MockitoExtension.class)
class PushRpcServiceImplTest {

    @Mock
    private INotificationPushManager notificationPushManager;

    private PushRpcServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PushRpcServiceImpl();
    }

    private void withPushManager() {
        ReflectionTestUtils.setField(service, "notificationPushManager", notificationPushManager);
    }

    @Test
    @DisplayName("pushAppMsg：存在推送管理器时委托处理")
    void pushAppMsgDelegatesWhenManagerPresent() {
        withPushManager();
        service.pushAppMsg(null, null, null, null);
        verify(notificationPushManager).mobilePush(null, null, null, null);
    }

    @Test
    @DisplayName("pushAppMsg：无推送管理器时抛出业务异常")
    void pushAppMsgThrowsWhenManagerAbsent() {
        assertThatThrownBy(() -> service.pushAppMsg(null, null, null, null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("pushAppMsgAsync：存在推送管理器时委托处理")
    void pushAppMsgAsyncDelegatesWhenManagerPresent() {
        withPushManager();
        service.pushAppMsgAsync(null, null, null, null);
        verify(notificationPushManager).mobilePushAsync(null, null, null, null);
    }

    @Test
    @DisplayName("pushAppMsgAsync：无推送管理器时仅记录日志")
    void pushAppMsgAsyncDoesNotThrowWhenManagerAbsent() {
        assertThatCode(() -> service.pushAppMsgAsync(null, null, null, null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("smsPush：存在推送管理器时委托处理")
    void smsPushDelegatesWhenManagerPresent() {
        withPushManager();
        service.smsPush(null);
        verify(notificationPushManager).smsPush(null);
    }

    @Test
    @DisplayName("smsPush：无推送管理器时抛出业务异常")
    void smsPushThrowsWhenManagerAbsent() {
        assertThatThrownBy(() -> service.smsPush(null)).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("smsPushAsync：存在推送管理器时委托处理")
    void smsPushAsyncDelegatesWhenManagerPresent() {
        withPushManager();
        service.smsPushAsync(null);
        verify(notificationPushManager).smsPushAsync(null);
    }

    @Test
    @DisplayName("smsPushAsync：无推送管理器时仅记录日志")
    void smsPushAsyncDoesNotThrowWhenManagerAbsent() {
        assertThatCode(() -> service.smsPushAsync(null)).doesNotThrowAnyException();
    }
}
