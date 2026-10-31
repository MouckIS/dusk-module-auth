package com.dusk.module.auth.authz.version;

import com.dusk.common.core.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link AuthzPermissionVersionService} 单元测试：版本首次分配、乐观冲突降级与入参校验。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthzPermissionVersionServiceTest {

    private static final Long ROLE_ID = 9L;

    @Mock
    private IAuthzRoleVersionRepository repository;

    private AuthzPermissionVersionService service;

    @BeforeEach
    void setUp() {
        service = new AuthzPermissionVersionService(repository);
    }

    @Test
    @DisplayName("首次分配：无版本记录时插入 1，Host 作用域由 null 租户生成")
    void shouldCreateFirstVersionForHost() {
        when(repository.findWithLockByScopeAndRoleId("HOST", ROLE_ID)).thenReturn(Optional.empty());
        when(repository.save(any(AuthzRoleVersion.class))).thenAnswer(inv -> inv.getArgument(0));

        long version = service.nextVersion(null, ROLE_ID);

        assertThat(version).isEqualTo(1L);
        ArgumentCaptor<AuthzRoleVersion> captor = ArgumentCaptor.forClass(AuthzRoleVersion.class);
        verify(repository).save(captor.capture());
        AuthzRoleVersion saved = captor.getValue();
        assertThat(saved.getTenantScope()).isEqualTo("HOST");
        assertThat(saved.getTenantId()).isNull();
        assertThat(saved.getRoleId()).isEqualTo(ROLE_ID);
        assertThat(saved.getPermissionVersion()).isEqualTo(1L);
    }

    @Test
    @DisplayName("首次分配：显式租户作用域与 tenantId 一并落库")
    void shouldCreateFirstVersionForTenantScope() {
        when(repository.findWithLockByScopeAndRoleId("TENANT:5", ROLE_ID)).thenReturn(Optional.empty());
        when(repository.save(any(AuthzRoleVersion.class))).thenAnswer(inv -> inv.getArgument(0));

        long version = service.nextVersion("TENANT:5", 5L, ROLE_ID);

        assertThat(version).isEqualTo(1L);
        ArgumentCaptor<AuthzRoleVersion> captor = ArgumentCaptor.forClass(AuthzRoleVersion.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getTenantScope()).isEqualTo("TENANT:5");
        assertThat(captor.getValue().getTenantId()).isEqualTo(5L);
    }

    @Test
    @DisplayName("已有记录：悲观锁读取后自增并保存")
    void shouldBumpExistingVersion() {
        AuthzRoleVersion row = new AuthzRoleVersion();
        row.setTenantScope("HOST");
        row.setRoleId(ROLE_ID);
        row.setPermissionVersion(4L);
        when(repository.findWithLockByScopeAndRoleId("HOST", ROLE_ID)).thenReturn(Optional.of(row));
        when(repository.save(any(AuthzRoleVersion.class))).thenAnswer(inv -> inv.getArgument(0));

        long version = service.nextVersion(null, ROLE_ID);

        assertThat(version).isEqualTo(5L);
        assertThat(row.getPermissionVersion()).isEqualTo(5L);
        verify(repository).save(row);
        verify(repository, never()).findWithLockByScopeAndRoleId("TENANT:5", ROLE_ID);
    }

    @Test
    @DisplayName("并发首次插入撞唯一索引：重读后在该行上自增")
    void shouldFallbackToBumpAfterIntegrityViolation() {
        AuthzRoleVersion row = new AuthzRoleVersion();
        row.setPermissionVersion(2L);
        when(repository.findWithLockByScopeAndRoleId("HOST", ROLE_ID))
                .thenReturn(Optional.empty(), Optional.of(row));
        when(repository.save(any(AuthzRoleVersion.class)))
                .thenThrow(new DataIntegrityViolationException("dup"))
                .thenAnswer(inv -> inv.getArgument(0));

        long version = service.nextVersion(null, ROLE_ID);

        assertThat(version).isEqualTo(3L);
        assertThat(row.getPermissionVersion()).isEqualTo(3L);
        verify(repository, times(2)).findWithLockByScopeAndRoleId("HOST", ROLE_ID);
        verify(repository, times(2)).save(any(AuthzRoleVersion.class));
    }

    @Test
    @DisplayName("并发冲突后仍读不到行：抛 IllegalStateException 保留原因")
    void shouldFailWhenRowMissingAfterConflict() {
        when(repository.findWithLockByScopeAndRoleId("HOST", ROLE_ID)).thenReturn(Optional.empty());
        when(repository.save(any(AuthzRoleVersion.class)))
                .thenThrow(new DataIntegrityViolationException("dup"));

        assertThatThrownBy(() -> service.nextVersion(null, ROLE_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("仍读不到行")
                .hasCauseInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("roleId 为空：直接拒绝，不触碰仓储")
    void shouldRejectNullRoleId() {
        assertThatThrownBy(() -> service.nextVersion(null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("roleId 不能为空");
        verify(repository, never()).findWithLockByScopeAndRoleId(any(), any());
    }

    @Test
    @DisplayName("非法作用域：在访问仓储前由 AuthzScope 拒绝")
    void shouldRejectInvalidScope() {
        assertThatThrownBy(() -> service.nextVersion("bad-scope", 5L, ROLE_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("非法的授权作用域");
        verify(repository, never()).findWithLockByScopeAndRoleId(any(), any());
    }
}
