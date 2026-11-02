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
 * {@link AuthzSubjectVersionService} 单元测试：代际分配、冲突降级与当前版本读取。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthzSubjectVersionServiceTest {

    private static final Long USER_ID = 11L;
    private static final String SCOPE = "TENANT:5";

    @Mock
    private IAuthzSubjectVersionRepository repository;

    private AuthzSubjectVersionService service;

    @BeforeEach
    void setUp() {
        service = new AuthzSubjectVersionService(repository);
    }

    @Test
    @DisplayName("首次分配：插入 1 并记录作用域")
    void shouldCreateFirstVersion() {
        when(repository.findWithLockByScopeAndUserId(SCOPE, USER_ID)).thenReturn(Optional.empty());
        when(repository.save(any(AuthzSubjectVersion.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.nextVersion(SCOPE, USER_ID)).isEqualTo(1L);

        ArgumentCaptor<AuthzSubjectVersion> captor = ArgumentCaptor.forClass(AuthzSubjectVersion.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getTenantScope()).isEqualTo(SCOPE);
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(captor.getValue().getSubjectVersion()).isEqualTo(1L);
    }

    @Test
    @DisplayName("已有记录：自增后保存")
    void shouldBumpExistingVersion() {
        AuthzSubjectVersion row = new AuthzSubjectVersion();
        row.setSubjectVersion(7L);
        when(repository.findWithLockByScopeAndUserId(SCOPE, USER_ID)).thenReturn(Optional.of(row));
        when(repository.save(any(AuthzSubjectVersion.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.nextVersion(SCOPE, USER_ID)).isEqualTo(8L);
        assertThat(row.getSubjectVersion()).isEqualTo(8L);
        verify(repository).save(row);
    }

    @Test
    @DisplayName("并发首次插入冲突：重读后自增")
    void shouldFallbackToBumpAfterIntegrityViolation() {
        AuthzSubjectVersion row = new AuthzSubjectVersion();
        row.setSubjectVersion(3L);
        when(repository.findWithLockByScopeAndUserId(SCOPE, USER_ID))
                .thenReturn(Optional.empty(), Optional.of(row));
        when(repository.save(any(AuthzSubjectVersion.class)))
                .thenThrow(new DataIntegrityViolationException("dup"))
                .thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.nextVersion(SCOPE, USER_ID)).isEqualTo(4L);
        verify(repository, times(2)).findWithLockByScopeAndUserId(SCOPE, USER_ID);
    }

    @Test
    @DisplayName("冲突后仍读不到行：抛 IllegalStateException")
    void shouldFailWhenRowMissingAfterConflict() {
        when(repository.findWithLockByScopeAndUserId(SCOPE, USER_ID)).thenReturn(Optional.empty());
        when(repository.save(any(AuthzSubjectVersion.class)))
                .thenThrow(new DataIntegrityViolationException("dup"));

        assertThatThrownBy(() -> service.nextVersion(SCOPE, USER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("仍读不到行")
                .hasCauseInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("userId 为空：拒绝分配")
    void shouldRejectNullUserId() {
        assertThatThrownBy(() -> service.nextVersion(SCOPE, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId 不能为空");
        verify(repository, never()).findWithLockByScopeAndUserId(any(), any());
    }

    @Test
    @DisplayName("非法作用域：拒绝分配")
    void shouldRejectInvalidScopeOnNextVersion() {
        assertThatThrownBy(() -> service.nextVersion("bad", USER_ID))
                .isInstanceOf(BusinessException.class);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("currentVersion：有记录返回当前值")
    void currentVersionShouldReturnStoredValue() {
        AuthzSubjectVersion row = new AuthzSubjectVersion();
        row.setSubjectVersion(12L);
        when(repository.findByTenantScopeAndUserId(SCOPE, USER_ID)).thenReturn(Optional.of(row));

        assertThat(service.currentVersion(SCOPE, USER_ID)).isEqualTo(12L);
    }

    @Test
    @DisplayName("currentVersion：无记录返回 0（从未发生过代际变更）")
    void currentVersionShouldReturnZeroWhenMissing() {
        when(repository.findByTenantScopeAndUserId(SCOPE, USER_ID)).thenReturn(Optional.empty());

        assertThat(service.currentVersion(SCOPE, USER_ID)).isZero();
    }

    @Test
    @DisplayName("currentVersion：userId 为空返回 0，不访问仓储")
    void currentVersionShouldReturnZeroForNullUser() {
        assertThat(service.currentVersion(SCOPE, null)).isZero();
        verify(repository, never()).findByTenantScopeAndUserId(any(), any());
    }

    @Test
    @DisplayName("currentVersion：非法作用域向上抛出")
    void currentVersionShouldRejectInvalidScope() {
        assertThatThrownBy(() -> service.currentVersion("bad", USER_ID))
                .isInstanceOf(BusinessException.class);
    }
}
