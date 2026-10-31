package com.dusk.module.auth.registry.rpc;

import com.dusk.common.core.auth.registry.ResourceSnapshot;
import com.dusk.common.core.auth.registry.SnapshotResource;
import com.dusk.common.rpc.authz.ResourceRegistrationResult;
import com.dusk.module.auth.registry.service.IResourceRegistryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * {@link PermissionRegistryRpcServiceImpl} 单元测试。
 *
 * <p>Dubbo Provider 只做两件事：拒绝空快照（稳定结果码，不抛异常）与把业务结果原样返回。
 * 结果码是 SDK readiness 的跨进程契约，必须逐字断言。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PermissionRegistryRpcServiceImplTest {

    @Mock
    private IResourceRegistryService resourceRegistryService;

    private PermissionRegistryRpcServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PermissionRegistryRpcServiceImpl(resourceRegistryService);
    }

    @Test
    @DisplayName("空快照直接返回 INVALID_SNAPSHOT，不进入注册服务")
    void rejectsNullSnapshot() {
        ResourceRegistrationResult result = service.registerSnapshot(null);

        assertThat(result.resultCode()).isEqualTo(ResourceRegistrationResult.INVALID_SNAPSHOT);
        assertThat(result.releaseId()).isNull();
        assertThat(result.readyForTraffic()).isFalse();
        assertThat(result.deterministic()).isTrue();
        verifyNoInteractions(resourceRegistryService);
    }

    @Test
    @DisplayName("非空快照委托给注册服务并返回其结果")
    void delegatesRegistration() {
        ResourceSnapshot snapshot = ResourceSnapshot.of("dusk-module-user", "1.0.0",
                List.of(SnapshotResource.of("GET", "/users", "p1")), List.of());
        ResourceRegistrationResult accepted = ResourceRegistrationResult.accepted("100", 3L);
        when(resourceRegistryService.registerSnapshot(snapshot)).thenReturn(accepted);

        assertThat(service.registerSnapshot(snapshot)).isSameAs(accepted);
        verify(resourceRegistryService).registerSnapshot(snapshot);
    }
}
