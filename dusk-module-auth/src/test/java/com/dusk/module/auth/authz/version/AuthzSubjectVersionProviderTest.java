package com.dusk.module.auth.authz.version;

import com.dusk.common.core.model.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AuthzSubjectVersionProvider} 单元测试：JWT 签发时的代际读取与降级语义。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthzSubjectVersionProviderTest {

    @Mock
    private AuthzSubjectVersionService versionService;

    private AuthzSubjectVersionProvider provider;

    private static UserContext user(Long id, Long tenantId) {
        UserContext user = new UserContext();
        user.setId(id);
        user.setTenantId(tenantId);
        return user;
    }

    @BeforeEach
    void setUp() {
        provider = new AuthzSubjectVersionProvider(versionService);
    }

    @Test
    @DisplayName("用户为空：返回 0，不访问版本服务")
    void shouldReturnZeroForNullUser() {
        assertThat(provider.currentSubjectVersion(null)).isZero();
        verify(versionService, never()).currentVersion(any(), any());
    }

    @Test
    @DisplayName("用户 ID 为空：返回 0，不访问版本服务")
    void shouldReturnZeroForNullUserId() {
        assertThat(provider.currentSubjectVersion(user(null, 5L))).isZero();
        verify(versionService, never()).currentVersion(any(), any());
    }

    @Test
    @DisplayName("租户用户：按 TENANT:<id> 作用域读取")
    void shouldReadTenantScopedVersion() {
        when(versionService.currentVersion("TENANT:5", 3L)).thenReturn(8L);

        assertThat(provider.currentSubjectVersion(user(3L, 5L))).isEqualTo(8L);
    }

    @Test
    @DisplayName("Host 用户（tenantId 为空）：按 HOST 作用域读取")
    void shouldReadHostScopedVersion() {
        when(versionService.currentVersion("HOST", 3L)).thenReturn(2L);

        assertThat(provider.currentSubjectVersion(user(3L, null))).isEqualTo(2L);
    }

    @Test
    @DisplayName("版本表不可用：签发放行返回 0，不阻断登录")
    void shouldFallbackToZeroOnFailure() {
        when(versionService.currentVersion("TENANT:5", 3L))
                .thenThrow(new IllegalStateException("db down"));

        assertThat(provider.currentSubjectVersion(user(3L, 5L))).isZero();
    }
}
