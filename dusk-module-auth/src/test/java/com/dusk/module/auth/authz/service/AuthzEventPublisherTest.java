package com.dusk.module.auth.authz.service;

import com.dusk.module.auth.authz.config.AuthzMqProperties;
import com.dusk.module.auth.authz.outbox.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link AuthzEventPublisher} 单元测试：Outbox 写入字段、开关语义与事务提交后唤醒。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthzEventPublisherTest {

    @Mock
    private IAuthzOutboxEventRepository repository;
    @Mock
    private ObjectProvider<AuthzOutboxPublisher> publisherProvider;
    @Mock
    private AuthzOutboxPublisher publisher;
    @Mock
    private ObjectMapper objectMapper;

    private AuthzMqProperties properties;
    private AuthzEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        properties = new AuthzMqProperties();
        properties.setEnabled(true);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"payload\":true}");
        when(publisherProvider.getIfAvailable()).thenReturn(publisher);
        eventPublisher = new AuthzEventPublisher(repository, properties, objectMapper, publisherProvider);
    }

    private void stubSaveWithId() {
        when(repository.save(any(AuthzOutboxEvent.class))).thenAnswer(invocation -> {
            AuthzOutboxEvent row = invocation.getArgument(0);
            row.setId(42L);
            return row;
        });
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private AuthzOutboxEvent savedRow() {
        ArgumentCaptor<AuthzOutboxEvent> captor = ArgumentCaptor.forClass(AuthzOutboxEvent.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("RESOURCE_CHANGED（三参重载）：revision 默认 0，立即唤醒发布器")
    void shouldPublishResourceChangedWithDefaultRevision() {
        stubSaveWithId();
        eventPublisher.resourceChanged("dusk-module-order", "rv-1", "1.0.0");

        AuthzOutboxEvent row = savedRow();
        assertThat(row.getEventType()).isEqualTo("RESOURCE_CHANGED");
        assertThat(row.getBizKey()).isEqualTo("RESOURCE_CHANGED:dusk-module-order:0");
        assertThat(row.getRefKey()).isEqualTo("dusk-module-order");
        assertThat(row.getRevision()).isZero();
        assertThat(row.getPayload()).isEqualTo("{\"payload\":true}");
        assertThat(row.getStatus()).isEqualTo(AuthzOutboxStatus.PENDING);
        assertThat(row.getProjectionStatus()).isEqualTo(AuthzOutboxProjectionStatus.PENDING);
        verify(publisher).publishNow(42L);
    }

    @Test
    @DisplayName("RESOURCE_CHANGED（四参重载）：保存 resourceRevision")
    void shouldPublishResourceChangedWithRevision() {
        stubSaveWithId();
        eventPublisher.resourceChanged("svc", "rv-2", "2.0.0", 9L);

        AuthzOutboxEvent row = savedRow();
        assertThat(row.getRevision()).isEqualTo(9L);
        assertThat(row.getBizKey()).isEqualTo("RESOURCE_CHANGED:svc:9");
    }

    @Test
    @DisplayName("RESOURCE_CHANGED：serviceId/resourceVersion 为空或空白时跳过")
    void shouldSkipInvalidResourceChanged() {
        eventPublisher.resourceChanged(null, "rv", "1.0.0");
        eventPublisher.resourceChanged("  ", "rv", "1.0.0");
        eventPublisher.resourceChanged("svc", null, "1.0.0");
        eventPublisher.resourceChanged("svc", "  ", "1.0.0");
        eventPublisher.resourceChanged(null, "rv", "1.0.0", 3L);

        verifyNoInteractions(repository);
        verify(publisher, never()).publishNow(any());
    }

    @Test
    @DisplayName("PERMISSION_CHANGED：租户 ID 转 TENANT:<id> 作用域")
    void shouldPublishPermissionChangedForTenant() {
        stubSaveWithId();
        eventPublisher.permissionChanged(5L, 7L, 3L);

        AuthzOutboxEvent row = savedRow();
        assertThat(row.getEventType()).isEqualTo("PERMISSION_CHANGED");
        assertThat(row.getRefKey()).isEqualTo("TENANT:5:7");
        assertThat(row.getBizKey()).isEqualTo("PERMISSION_CHANGED:TENANT:5:7:3");
        assertThat(row.getRevision()).isEqualTo(3L);
    }

    @Test
    @DisplayName("PERMISSION_CHANGED：Host 角色使用 HOST 而非 null，避免事件被静默跳过")
    void shouldPublishPermissionChangedForHost() {
        stubSaveWithId();
        eventPublisher.permissionChanged(null, 7L, 1L);

        AuthzOutboxEvent row = savedRow();
        assertThat(row.getRefKey()).isEqualTo("HOST:7");
        assertThat(row.getBizKey()).isEqualTo("PERMISSION_CHANGED:HOST:7:1");
    }

    @Test
    @DisplayName("PERMISSION_CHANGED：显式作用域优先")
    void shouldPublishPermissionChangedWithExplicitScope() {
        stubSaveWithId();
        eventPublisher.permissionChanged("TENANT:9", 9L, 8L, 2L);

        assertThat(savedRow().getRefKey()).isEqualTo("TENANT:9:8");
    }

    @Test
    @DisplayName("PERMISSION_CHANGED：作用域为空/空白或 roleId 为空时跳过")
    void shouldSkipInvalidPermissionChanged() {
        eventPublisher.permissionChanged(null, 5L, 7L, 1L);
        eventPublisher.permissionChanged("  ", 5L, 7L, 1L);
        eventPublisher.permissionChanged("HOST", 5L, null, 1L);

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("SUBJECT_CHANGED：保存代际与作用域")
    void shouldPublishSubjectChanged() {
        stubSaveWithId();
        eventPublisher.subjectChanged("TENANT:5", 7L, 4L, false);

        AuthzOutboxEvent row = savedRow();
        assertThat(row.getEventType()).isEqualTo("SUBJECT_CHANGED");
        assertThat(row.getRefKey()).isEqualTo("TENANT:5:7");
        assertThat(row.getBizKey()).isEqualTo("SUBJECT_CHANGED:TENANT:5:7:4");
        assertThat(row.getRevision()).isEqualTo(4L);
    }

    @Test
    @DisplayName("SUBJECT_CHANGED：作用域为空/空白或 userId 为空时跳过")
    void shouldSkipInvalidSubjectChanged() {
        eventPublisher.subjectChanged(null, 7L, 1L, true);
        eventPublisher.subjectChanged("  ", 7L, 1L, true);
        eventPublisher.subjectChanged("HOST", null, 1L, true);

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("总闸关闭：不写 Outbox、不唤醒发布器")
    void shouldSkipWhenDisabled() {
        properties.setEnabled(false);

        eventPublisher.permissionChanged(5L, 7L, 1L);

        verifyNoInteractions(repository);
        verify(publisher, never()).publishNow(any());
    }

    @Test
    @DisplayName("Outbox 开关关闭：同样不落库")
    void shouldSkipWhenOutboxDisabled() {
        properties.getOutbox().setEnabled(false);

        eventPublisher.subjectChanged("HOST", 7L, 1L, true);

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("载荷序列化失败：抛 IllegalStateException 让业务事务暴露问题")
    void shouldFailFastOnSerializationError() {
        when(objectMapper.writeValueAsString(any())).thenThrow(new IllegalStateException("json boom"));

        assertThatThrownBy(() -> eventPublisher.permissionChanged(5L, 7L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("事件载荷序列化失败")
                .hasCauseInstanceOf(IllegalStateException.class);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("事务活跃：事件先落库，afterCommit 才唤醒发布器")
    void shouldWakeUpOnlyAfterCommit() {
        stubSaveWithId();
        TransactionSynchronizationManager.initSynchronization();

        eventPublisher.permissionChanged(5L, 7L, 1L);

        verify(repository).save(any(AuthzOutboxEvent.class));
        verify(publisher, never()).publishNow(any());

        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }

        verify(publisher).publishNow(42L);
    }

    @Test
    @DisplayName("事务活跃但发布器缺失：afterCommit 静默跳过，不影响提交")
    void shouldTolerateMissingPublisherAfterCommit() {
        stubSaveWithId();
        when(publisherProvider.getIfAvailable()).thenReturn(null);
        TransactionSynchronizationManager.initSynchronization();

        eventPublisher.subjectChanged("HOST", 7L, 1L, true);

        assertThatCode(() -> {
            for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCommit();
            }
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("保存后主键为空：不注册唤醒（交给轮询兜底）")
    void shouldSkipWakeUpWhenIdMissing() {
        when(repository.save(any(AuthzOutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        eventPublisher.subjectChanged("HOST", 7L, 1L, true);

        verify(publisher, never()).publishNow(any());
    }

    @Test
    @DisplayName("Outbox 写入异常：向上抛出以回滚业务事务")
    void shouldPropagateRepositoryFailure() {
        when(repository.save(any(AuthzOutboxEvent.class))).thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> eventPublisher.resourceChanged("svc", "rv", "1.0.0"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("db down");
    }

    @Test
    @DisplayName("未开启事务：直接唤醒发布器（如单测或非事务调用点）")
    void shouldWakeUpImmediatelyWithoutTransaction() {
        stubSaveWithId();
        eventPublisher.subjectChanged("HOST", 7L, 1L, true);

        verify(publisher).publishNow(42L);
    }
}
