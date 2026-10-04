package com.dusk.module.auth.service.impl;

import com.dusk.module.auth.impl.DefaultAuthProvider;
import com.dusk.module.auth.service.IAuthRpcService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

/**
 * {@link DefaultAuthProvider} 单元测试
 * 覆盖 provideAuthInfo 成功路径与异常重试路径
 */
@DisplayName("DefaultAuthProvider 单元测试")
class DefaultAuthProviderTest {

    @Test
    @DisplayName("provideAuthInfo: 正常调用 authService 推送权限清单")
    void provideAuthInfo_success() {
        IAuthRpcService authService = mock(IAuthRpcService.class);
        DefaultAuthProvider provider = spy(new DefaultAuthProvider());
        provider.authService = authService;

        provider.provideAuthInfo("app", List.of(), Map.of(), Map.of());

        verify(authService).provideAuthInfo("app", List.of(), Map.of(), Map.of());
    }

    @Test
    @DisplayName("provideAuthInfo: 推送异常后记录日志并递归重试一次")
    void provideAuthInfo_retryOnException() throws InterruptedException {
        IAuthRpcService authService = mock(IAuthRpcService.class);
        DefaultAuthProvider provider = new DefaultAuthProvider();
        provider.authService = authService;

        // 必须存根协作者 authService：若存根 provider 自身的 provideAuthInfo，会替换掉真实方法体，
        // 导致 catch 中的递归重试逻辑完全不执行。
        // 第一次推送抛异常触发重试，第二次正常返回。
        willThrow(new RuntimeException("push failed"))
                .willDoNothing()
                .given(authService).provideAuthInfo(any(), any(), any(), any());

        // 在独立线程中执行，以便打断 10s 的 sleep，避免测试长时间阻塞
        Thread t = new Thread(() -> provider.provideAuthInfo("app", List.of(), Map.of(), Map.of()));
        t.start();
        Thread.sleep(300);
        t.interrupt();
        t.join(5000);
        assertFalse(t.isAlive(), "重试结束后工作线程应已终止");

        // 首次调用抛异常，递归重试成功 -> 共调用两次
        verify(authService, times(2)).provideAuthInfo(any(), any(), any(), any());
    }
}
