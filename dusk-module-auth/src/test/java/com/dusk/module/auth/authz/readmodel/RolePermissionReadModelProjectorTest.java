package com.dusk.module.auth.authz.readmodel;

import com.dusk.common.rpc.authz.RolePermissionSnapshot;
import com.dusk.module.auth.authz.config.AuthzMqProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link RolePermissionReadModelProjector} 单元测试：键格式、版本字段与投影结果语义。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RolePermissionReadModelProjectorTest {

    private static final String SCOPE = "TENANT:1";
    private static final Long ROLE_ID = 9L;

    @Mock
    private AuthorizationReadModelResolver resolver;

    @Mock
    private RedisVersionedWriter writer;

    private AuthzMqProperties properties;
    private ObjectMapper objectMapper;
    private RolePermissionReadModelProjector projector;

    @BeforeEach
    void setUp() {
        properties = new AuthzMqProperties();
        objectMapper = new ObjectMapper();
        projector = new RolePermissionReadModelProjector(resolver, writer, properties, objectMapper);
    }

    @Test
    @DisplayName("roleId 为空：拒绝投影，不访问解析器")
    void shouldRejectNullRoleId() {
        assertThat(projector.project(SCOPE, null)).isFalse();
        verify(resolver, never()).resolveRolePermissions(any(), any());
    }

    @Test
    @DisplayName("作用域为空：拒绝投影")
    void shouldRejectNullScope() {
        assertThat(projector.project(null, ROLE_ID)).isFalse();
        verify(resolver, never()).resolveRolePermissions(any(), any());
    }

    @Test
    @DisplayName("作用域为空白：拒绝投影")
    void shouldRejectBlankScope() {
        assertThat(projector.project("  ", ROLE_ID)).isFalse();
        verify(resolver, never()).resolveRolePermissions(any(), any());
    }

    @Test
    @DisplayName("解析结果为空（角色不存在/作用域不匹配）：投影失败，不写 Redis")
    void shouldFailWhenSnapshotMissing() {
        when(resolver.resolveRolePermissions(SCOPE, ROLE_ID)).thenReturn(Optional.empty());

        assertThat(projector.project(SCOPE, ROLE_ID)).isFalse();
        verify(writer, never()).write(anyString(), anyString(), anyLong(), anyString(), any());
    }

    @Test
    @DisplayName("解析成功：按 permissionVersion 写入并序列化网关约定 JSON")
    void shouldWriteVersionedPayload() {
        when(resolver.resolveRolePermissions(SCOPE, ROLE_ID))
                .thenReturn(Optional.of(new RolePermissionSnapshot(SCOPE, ROLE_ID, 7L,
                        new LinkedHashSet<>(List.of("system:user:query", "system:user:add")))));
        when(writer.write(anyString(), anyString(), anyLong(), anyString(), any())).thenReturn(true);

        assertThat(projector.project(SCOPE, ROLE_ID)).isTrue();

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(writer).write(org.mockito.ArgumentMatchers.eq("auth:authorization:TENANT:1:role:9"),
                org.mockito.ArgumentMatchers.eq("permissionVersion"), org.mockito.ArgumentMatchers.eq(7L),
                payload.capture(), org.mockito.ArgumentMatchers.eq(Duration.ofMinutes(30)));
        assertThat(payload.getValue())
                .contains("\"tenantScope\":\"TENANT:1\"")
                .contains("\"roleId\":9")
                .contains("\"permissionVersion\":7")
                .contains("system:user:query")
                .contains("system:user:add");
    }

    @Test
    @DisplayName("写入器返回 false：投影结果透传为失败，触发 Outbox 重试")
    void shouldPropagateWriterFailure() {
        when(resolver.resolveRolePermissions(SCOPE, ROLE_ID))
                .thenReturn(Optional.of(new RolePermissionSnapshot(SCOPE, ROLE_ID, 7L, new LinkedHashSet<>())));
        when(writer.write(anyString(), anyString(), anyLong(), anyString(), any())).thenReturn(false);

        assertThat(projector.project(SCOPE, ROLE_ID)).isFalse();
    }

    @Test
    @DisplayName("解析异常：记录日志并返回失败，不向上冒泡")
    void shouldSwallowResolverException() {
        when(resolver.resolveRolePermissions(SCOPE, ROLE_ID)).thenThrow(new IllegalStateException("db down"));

        assertThat(projector.project(SCOPE, ROLE_ID)).isFalse();
    }

    @Test
    @DisplayName("序列化异常：返回失败，不写 Redis")
    void shouldSwallowSerializationException() {
        ObjectMapper failing = mock(ObjectMapper.class);
        when(failing.writeValueAsString(any())).thenThrow(new IllegalStateException("json"));
        RolePermissionReadModelProjector failingProjector =
                new RolePermissionReadModelProjector(resolver, writer, properties, failing);
        when(resolver.resolveRolePermissions(SCOPE, ROLE_ID))
                .thenReturn(Optional.of(new RolePermissionSnapshot(SCOPE, ROLE_ID, 7L, new LinkedHashSet<>())));

        assertThat(failingProjector.project(SCOPE, ROLE_ID)).isFalse();
        verify(writer, never()).write(anyString(), anyString(), anyLong(), anyString(), any());
    }
}
