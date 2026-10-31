package com.dusk.module.auth.authz.readmodel;

import com.dusk.common.rpc.authz.SubjectSnapshot;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link SubjectReadModelProjector} 单元测试：账号状态读模型的键、TTL 与字段约定。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubjectReadModelProjectorTest {

    private static final String SCOPE = "TENANT:5";
    private static final Long USER_ID = 7L;

    @Mock
    private AuthorizationReadModelResolver resolver;

    @Mock
    private RedisVersionedWriter writer;

    private AuthzMqProperties properties;
    private ObjectMapper objectMapper;
    private SubjectReadModelProjector projector;

    @BeforeEach
    void setUp() {
        properties = new AuthzMqProperties();
        objectMapper = new ObjectMapper();
        projector = new SubjectReadModelProjector(resolver, writer, properties, objectMapper);
    }

    @Test
    @DisplayName("userId 为空：拒绝投影")
    void shouldRejectNullUserId() {
        assertThat(projector.project(SCOPE, null)).isFalse();
        verify(resolver, never()).resolveSubject(any(), any());
    }

    @Test
    @DisplayName("作用域为空：拒绝投影")
    void shouldRejectNullScope() {
        assertThat(projector.project(null, USER_ID)).isFalse();
        verify(resolver, never()).resolveSubject(any(), any());
    }

    @Test
    @DisplayName("作用域为空白：拒绝投影")
    void shouldRejectBlankScope() {
        assertThat(projector.project(" ", USER_ID)).isFalse();
        verify(resolver, never()).resolveSubject(any(), any());
    }

    @Test
    @DisplayName("解析结果为空：投影失败，不写 Redis")
    void shouldFailWhenSnapshotMissing() {
        when(resolver.resolveSubject(SCOPE, USER_ID)).thenReturn(Optional.empty());

        assertThat(projector.project(SCOPE, USER_ID)).isFalse();
        verify(writer, never()).write(anyString(), anyString(), anyLong(), anyString(), any());
    }

    @Test
    @DisplayName("解析成功：写入账号状态与角色 ID，TTL 取 subjectReadModelTtl")
    void shouldWriteVersionedPayload() {
        when(resolver.resolveSubject(SCOPE, USER_ID))
                .thenReturn(Optional.of(new SubjectSnapshot(SCOPE, USER_ID, 11L, false,
                        new LinkedHashSet<>(List.of(3L, 4L)))));
        when(writer.write(anyString(), anyString(), anyLong(), anyString(), any())).thenReturn(true);

        assertThat(projector.project(SCOPE, USER_ID)).isTrue();

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(writer).write(org.mockito.ArgumentMatchers.eq("auth:subject:TENANT:5:user:7"),
                org.mockito.ArgumentMatchers.eq("subjectVersion"), org.mockito.ArgumentMatchers.eq(11L),
                payload.capture(), org.mockito.ArgumentMatchers.eq(Duration.ofMinutes(1)));
        assertThat(payload.getValue())
                .contains("\"tenantScope\":\"TENANT:5\"")
                .contains("\"userId\":7")
                .contains("\"subjectVersion\":11")
                .contains("\"enabled\":false")
                .contains("3")
                .contains("4");
    }

    @Test
    @DisplayName("写入器返回 false：投影失败")
    void shouldPropagateWriterFailure() {
        when(resolver.resolveSubject(SCOPE, USER_ID))
                .thenReturn(Optional.of(new SubjectSnapshot(SCOPE, USER_ID, 1L, true, new LinkedHashSet<>())));
        when(writer.write(anyString(), anyString(), anyLong(), anyString(), any())).thenReturn(false);

        assertThat(projector.project(SCOPE, USER_ID)).isFalse();
    }

    @Test
    @DisplayName("解析异常：吞掉并返回失败")
    void shouldSwallowResolverException() {
        when(resolver.resolveSubject(SCOPE, USER_ID)).thenThrow(new IllegalStateException("db down"));

        assertThat(projector.project(SCOPE, USER_ID)).isFalse();
    }

    @Test
    @DisplayName("序列化异常：返回失败，不写 Redis")
    void shouldSwallowSerializationException() {
        ObjectMapper failing = mock(ObjectMapper.class);
        when(failing.writeValueAsString(any())).thenThrow(new IllegalStateException("json"));
        SubjectReadModelProjector failingProjector =
                new SubjectReadModelProjector(resolver, writer, properties, failing);
        when(resolver.resolveSubject(SCOPE, USER_ID))
                .thenReturn(Optional.of(new SubjectSnapshot(SCOPE, USER_ID, 1L, true, new LinkedHashSet<>())));

        assertThat(failingProjector.project(SCOPE, USER_ID)).isFalse();
        verify(writer, never()).write(anyString(), anyString(), anyLong(), anyString(), any());
    }
}
