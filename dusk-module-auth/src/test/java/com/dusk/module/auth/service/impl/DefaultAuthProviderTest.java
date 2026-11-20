package com.dusk.module.auth.service.impl;

import com.dusk.module.auth.adapter.DefaultAuthProvider;
import com.dusk.module.auth.service.IAuthRpcService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

/**
 * {@link DefaultAuthProvider} 单元测试
 * 覆盖 provideAuthInfo 成功路径与异常不再重试的路径
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
    @DisplayName("provideAuthInfo: 推送异常时只记录日志，不再无限重试")
    void provideAuthInfo_doesNotRetryOnException() {
        IAuthRpcService authService = mock(IAuthRpcService.class);
        DefaultAuthProvider provider = new DefaultAuthProvider();
        provider.authService = authService;

        willThrow(new RuntimeException("push failed"))
                .given(authService).provideAuthInfo(any(), any(), any(), any());

        // 单体化后 authService 是同进程本地 bean，失败不再视为可重试的网络抖动，
        // 因此实现里已去掉 while(true)+sleep(10000) 的无限重试：本方法必须同步返回且不抛异常。
        assertDoesNotThrow(() -> provider.provideAuthInfo("app", List.of(), Map.of(), Map.of()));

        // 只调用一次：既不重试，也不会永久占用 @Async 线程
        verify(authService, times(1)).provideAuthInfo(any(), any(), any(), any());
    }
}
