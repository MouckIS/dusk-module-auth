package com.dusk.module.auth.authz.outbox;

import com.dusk.common.mqs.core.CommonSendResult;
import com.dusk.common.mqs.core.MessageSender;
import com.dusk.common.mqs.core.MqMessage;
import com.dusk.module.auth.authz.config.AuthzMqProperties;
import com.dusk.module.auth.authz.event.PermissionChangedEvent;
import com.dusk.module.auth.authz.event.ResourceChangedEvent;
import com.dusk.module.auth.authz.event.SubjectChangedEvent;
import com.dusk.module.auth.authz.readmodel.RolePermissionReadModelProjector;
import com.dusk.module.auth.authz.readmodel.SubjectReadModelProjector;
import com.dusk.module.auth.registry.service.ResourceReadModelProjector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link AuthzOutboxPublisher} 单元测试，覆盖《权限优化方案-整理版》6.2 / 6.3：
 * 领取与租约、投影先行、发送结果分类、退避与终态。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthzOutboxPublisherTest {

    private static final String RESOURCE_TOPIC = "RESOURCE_CHANGED";
    private static final String PERMISSION_TOPIC = "PERMISSION_CHANGED";
    private static final String SUBJECT_TOPIC = "SUBJECT_CHANGED";

    @Mock
    private IAuthzOutboxEventRepository repository;
    @Mock
    private ObjectProvider<MessageSender> senderProvider;
    @Mock
    private ObjectProvider<ResourceReadModelProjector> resourceProvider;
    @Mock
    private ObjectProvider<RolePermissionReadModelProjector> roleProvider;
    @Mock
    private ObjectProvider<SubjectReadModelProjector> subjectProvider;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private MessageSender sender;
    @Mock
    private ResourceReadModelProjector resourceProjector;
    @Mock
    private RolePermissionReadModelProjector roleProjector;
    @Mock
    private SubjectReadModelProjector subjectProjector;

    private AuthzMqProperties properties;
    private ObjectMapper objectMapper;
    private AuthzOutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        properties = new AuthzMqProperties();
        properties.setEnabled(true);
        properties.getOutbox().setEnabled(true);
        objectMapper = new ObjectMapper();
        publisher = new AuthzOutboxPublisher(repository, properties, senderProvider, resourceProvider,
                roleProvider, subjectProvider, objectMapper, jdbcTemplate);
    }

    @AfterEach
    void tearDown() {
        publisher.destroy();
    }

    private void stubClaim(List<Long> ids) {
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), any(), any(), any(), any(), any()))
                .thenReturn(ids);
    }

    private void stubClaimUpdate(int rows) {
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any())).thenReturn(rows);
    }

    private String payloadFor(String type) {
        return switch (type) {
            case RESOURCE_TOPIC -> objectMapper.writeValueAsString(
                    ResourceChangedEvent.of("svc", "rv-1", "rel-1", 3L));
            case PERMISSION_TOPIC -> objectMapper.writeValueAsString(
                    PermissionChangedEvent.of("TENANT:5", 5L, 7L, 9L));
            case SUBJECT_TOPIC -> objectMapper.writeValueAsString(
                    SubjectChangedEvent.of("TENANT:5", 7L, 4L, true));
            default -> "raw-payload";
        };
    }

    private AuthzOutboxEvent event(long id, String type, String refKey) {
        AuthzOutboxEvent event = new AuthzOutboxEvent();
        event.setId(id);
        event.setEventType(type);
        event.setBizKey("biz-" + id);
        event.setRefKey(refKey);
        event.setPayload(payloadFor(type));
        return event;
    }

    private CommonSendResult ack() {
        return CommonSendResult.ok("msg-1", "topic", "SEND_OK", 1L);
    }

    private void deliver(AuthzOutboxEvent event, MessageSender messageSender) {
        ReflectionTestUtils.invokeMethod(publisher, "deliver", event, messageSender);
    }

    @SuppressWarnings("unchecked")
    private List<MqMessage<Object>> capturedMessages(int expected) {
        ArgumentCaptor<MqMessage<Object>> captor = ArgumentCaptor.forClass(MqMessage.class);
        verify(sender, times(expected)).send(captor.capture());
        return captor.getAllValues();
    }

    @Test
    @DisplayName("总闸关闭：轮询直接返回，不访问数据库")
    void pollShouldSkipWhenDisabled() {
        properties.setEnabled(false);

        publisher.poll();

        verifyNoInteractions(jdbcTemplate, repository);
    }

    @Test
    @DisplayName("Outbox 开关关闭：轮询直接返回")
    void pollShouldSkipWhenOutboxDisabled() {
        properties.getOutbox().setEnabled(false);

        publisher.poll();

        verifyNoInteractions(jdbcTemplate, repository);
    }

    @Test
    @DisplayName("没有到期事件：直接返回")
    void pollShouldReturnWhenNoDueEvents() {
        stubClaim(List.of());

        publisher.poll();

        verify(repository, never()).findAllById(any());
    }

    @Test
    @DisplayName("领取返回 null：按空批处理")
    void pollShouldTreatNullClaimAsEmpty() {
        stubClaim(null);

        assertThatCode(() -> publisher.poll()).doesNotThrowAnyException();
        verify(repository, never()).findAllById(any());
    }

    @Test
    @DisplayName("领取异常：吞掉，下一轮继续")
    void pollShouldSwallowClaimFailure() {
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), any(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("db down"));

        assertThatCode(() -> publisher.poll()).doesNotThrowAnyException();
        verify(repository, never()).findAllById(any());
    }

    @Test
    @DisplayName("MessageSender 缺失：释放租约保留 PENDING，重复告警被限流")
    void pollShouldReleaseLeasesWhenSenderMissing() {
        stubClaim(List.of(1L, 2L));
        when(senderProvider.getIfAvailable()).thenReturn(null);

        publisher.poll();
        publisher.poll();

        assertThat(countVarargsUpdates(2)).isEqualTo(4);
        verify(repository, never()).findAllById(any());
        verify(sender, never()).send(any());
    }

    @Test
    @DisplayName("领取到 id 但事件已被删除：跳过投递")
    void pollShouldSkipMissingRows() {
        stubClaim(List.of(1L));
        stubClaimUpdate(1);
        when(senderProvider.getIfAvailable()).thenReturn(sender);
        when(repository.findAllById(List.of(1L))).thenReturn(List.of());

        publisher.poll();

        verify(sender, never()).send(any());
    }

    @Test
    @DisplayName("RESOURCE_CHANGED：投影成功后发送，落 SENT 并清租约")
    void pollShouldProjectAndSendResourceChanged() {
        AuthzOutboxEvent event = event(1L, RESOURCE_TOPIC, "svc");
        stubClaim(List.of(1L));
        stubClaimUpdate(1);
        when(senderProvider.getIfAvailable()).thenReturn(sender);
        when(repository.findAllById(List.of(1L))).thenReturn(List.of(event));
        when(resourceProvider.getIfAvailable()).thenReturn(resourceProjector);
        when(resourceProjector.project("svc")).thenReturn(true);
        when(sender.send(any())).thenReturn(ack());

        publisher.poll();

        assertThat(event.getStatus()).isEqualTo(AuthzOutboxStatus.SENT);
        assertThat(event.getProjectionStatus()).isEqualTo(AuthzOutboxProjectionStatus.PROJECTED);
        assertThat(event.getProjectedAt()).isNotNull();
        assertThat(event.getSentAt()).isNotNull();
        assertThat(event.getLastError()).isNull();
        verify(repository).save(event);
        assertThat(countVarargsUpdates(2)).isEqualTo(1);

        MqMessage<Object> message = capturedMessages(1).getFirst();
        assertThat(message.getTopic()).isEqualTo(RESOURCE_TOPIC);
        assertThat(message.getBizKey()).isEqualTo("biz-1");
        assertThat(message.getKeys()).isEqualTo("biz-1");
        assertThat(message.getPayload()).isInstanceOf(ResourceChangedEvent.class);
    }

    @Test
    @DisplayName("PERMISSION_CHANGED：scope:roleId 拆解后投影并发送")
    void pollShouldProjectAndSendPermissionChanged() {
        AuthzOutboxEvent event = event(1L, PERMISSION_TOPIC, "TENANT:5:7");
        stubClaim(List.of(1L));
        stubClaimUpdate(1);
        when(senderProvider.getIfAvailable()).thenReturn(sender);
        when(repository.findAllById(List.of(1L))).thenReturn(List.of(event));
        when(roleProvider.getIfAvailable()).thenReturn(roleProjector);
        when(roleProjector.project("TENANT:5", 7L)).thenReturn(true);
        when(sender.send(any())).thenReturn(ack());

        publisher.poll();

        assertThat(event.getStatus()).isEqualTo(AuthzOutboxStatus.SENT);
        assertThat(capturedMessages(1).getFirst().getPayload())
                .isInstanceOf(PermissionChangedEvent.class);
        verify(roleProjector).project("TENANT:5", 7L);
    }

    @Test
    @DisplayName("SUBJECT_CHANGED：scope:userId 拆解后投影并发送")
    void pollShouldProjectAndSendSubjectChanged() {
        AuthzOutboxEvent event = event(1L, SUBJECT_TOPIC, "TENANT:5:7");
        stubClaim(List.of(1L));
        stubClaimUpdate(1);
        when(senderProvider.getIfAvailable()).thenReturn(sender);
        when(repository.findAllById(List.of(1L))).thenReturn(List.of(event));
        when(subjectProvider.getIfAvailable()).thenReturn(subjectProjector);
        when(subjectProjector.project("TENANT:5", 7L)).thenReturn(true);
        when(sender.send(any())).thenReturn(ack());

        publisher.poll();

        assertThat(event.getStatus()).isEqualTo(AuthzOutboxStatus.SENT);
        assertThat(capturedMessages(1).getFirst().getPayload())
                .isInstanceOf(SubjectChangedEvent.class);
    }

    @Test
    @DisplayName("未知事件类型：跳过投影，按原始文本投递（链路不被未来事件类型阻塞）")
    void pollShouldSendUnknownTypeAsRawPayload() {
        AuthzOutboxEvent event = event(1L, "FUTURE_EVENT", "whatever");
        stubClaim(List.of(1L));
        stubClaimUpdate(1);
        when(senderProvider.getIfAvailable()).thenReturn(sender);
        when(repository.findAllById(List.of(1L))).thenReturn(List.of(event));
        when(sender.send(any())).thenReturn(ack());

        publisher.poll();

        assertThat(event.getStatus()).isEqualTo(AuthzOutboxStatus.SENT);
        assertThat(capturedMessages(1).getFirst().getPayload()).isEqualTo("raw-payload");
        verifyNoInteractions(resourceProvider, roleProvider, subjectProvider);
    }

    @Test
    @DisplayName("按 id 升序投递，即使仓储返回乱序")
    void pollShouldDeliverInIdOrder() {
        AuthzOutboxEvent first = event(1L, "FUTURE_EVENT", "a");
        AuthzOutboxEvent second = event(2L, "FUTURE_EVENT", "b");
        stubClaim(List.of(2L, 1L));
        stubClaimUpdate(1);
        when(senderProvider.getIfAvailable()).thenReturn(sender);
        when(repository.findAllById(List.of(2L, 1L))).thenReturn(List.of(second, first));
        when(sender.send(any())).thenReturn(ack());

        publisher.poll();

        InOrder order = inOrder(sender);
        order.verify(sender).send(argThat((MqMessage<Object> message) -> "biz-1".equals(message.getBizKey())));
        order.verify(sender).send(argThat((MqMessage<Object> message) -> "biz-2".equals(message.getBizKey())));
    }

    @Test
    @DisplayName("投影失败：不发送事件，置 FAILED 投影状态并退避重试")
    void shouldNotSendWhenProjectionFails() {
        AuthzOutboxEvent event = event(1L, RESOURCE_TOPIC, "svc");
        when(resourceProvider.getIfAvailable()).thenReturn(resourceProjector);
        when(resourceProjector.project("svc")).thenReturn(false);

        deliver(event, sender);

        assertThat(event.getProjectionStatus()).isEqualTo(AuthzOutboxProjectionStatus.FAILED);
        assertThat(event.getStatus()).isEqualTo(AuthzOutboxStatus.PENDING);
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getNextRetryAt()).isNotNull();
        assertThat(event.getLastError()).isEqualTo("读模型投影失败");
        assertThat(event.getSentAt()).isNull();
        verify(sender, never()).send(any());
        verify(repository).save(event);
    }

    @Test
    @DisplayName("投影器缺失：视为投影失败，禁止发送")
    void shouldFailWhenProjectorMissing() {
        AuthzOutboxEvent resource = event(1L, RESOURCE_TOPIC, "svc");
        when(resourceProvider.getIfAvailable()).thenReturn(null);
        deliver(resource, sender);
        assertThat(resource.getProjectionStatus()).isEqualTo(AuthzOutboxProjectionStatus.FAILED);
        verify(sender, never()).send(any());
    }

    @Test
    @DisplayName("PERMISSION_CHANGED 的 refKey 非法：投影失败且不发送")
    void shouldRejectInvalidPermissionRefKey() {
        when(roleProvider.getIfAvailable()).thenReturn(roleProjector);
        when(roleProjector.project(anyString(), any())).thenReturn(true);

        AuthzOutboxEvent nullKey = event(1L, PERMISSION_TOPIC, null);
        AuthzOutboxEvent noSeparator = event(2L, PERMISSION_TOPIC, "roleOnly");
        AuthzOutboxEvent emptyScope = event(3L, PERMISSION_TOPIC, ":7");
        AuthzOutboxEvent badId = event(4L, PERMISSION_TOPIC, "TENANT:5:abc");

        deliver(nullKey, sender);
        deliver(noSeparator, sender);
        deliver(emptyScope, sender);
        deliver(badId, sender);

        assertThat(List.of(nullKey, noSeparator, emptyScope, badId))
                .allSatisfy(event -> {
                    assertThat(event.getProjectionStatus()).isEqualTo(AuthzOutboxProjectionStatus.FAILED);
                    assertThat(event.getAttempts()).isEqualTo(1);
                });
        verify(sender, never()).send(any());
        verify(roleProjector, never()).project(any(), any());
    }

    @Test
    @DisplayName("SUBJECT_CHANGED 的 refKey 非法：投影失败且不发送")
    void shouldRejectInvalidSubjectRefKey() {
        when(subjectProvider.getIfAvailable()).thenReturn(subjectProjector);
        when(subjectProjector.project(anyString(), any())).thenReturn(true);

        AuthzOutboxEvent nullKey = event(1L, SUBJECT_TOPIC, null);
        AuthzOutboxEvent noSeparator = event(2L, SUBJECT_TOPIC, "userOnly");
        AuthzOutboxEvent emptyScope = event(3L, SUBJECT_TOPIC, ":7");
        AuthzOutboxEvent badId = event(4L, SUBJECT_TOPIC, "TENANT:5:abc");

        deliver(nullKey, sender);
        deliver(noSeparator, sender);
        deliver(emptyScope, sender);
        deliver(badId, sender);

        assertThat(List.of(nullKey, noSeparator, emptyScope, badId))
                .allSatisfy(event -> assertThat(event.getProjectionStatus())
                        .isEqualTo(AuthzOutboxProjectionStatus.FAILED));
        verify(sender, never()).send(any());
        verify(subjectProjector, never()).project(any(), any());
    }

    @Test
    @DisplayName("Broker 明确拒绝：置 FAILED 终态，不自动重试")
    void shouldMarkRejectedAsTerminal() {
        AuthzOutboxEvent event = event(1L, RESOURCE_TOPIC, "svc");
        when(resourceProvider.getIfAvailable()).thenReturn(resourceProjector);
        when(resourceProjector.project("svc")).thenReturn(true);
        when(sender.send(any())).thenReturn(CommonSendResult.rejected("msg-1", "topic", 2L));

        deliver(event, sender);

        assertThat(event.getStatus()).isEqualTo(AuthzOutboxStatus.FAILED);
        assertThat(event.getAttempts()).isZero();
        assertThat(event.getNextRetryAt()).isNull();
        assertThat(event.getLastError()).contains("Broker 拒绝").contains("REJECTED");
        verify(repository).save(event);
    }

    @Test
    @DisplayName("Broker 未确认：保留 PENDING 并退避重试")
    void shouldRetryWhenNotAcknowledged() {
        AuthzOutboxEvent event = event(1L, RESOURCE_TOPIC, "svc");
        when(resourceProvider.getIfAvailable()).thenReturn(resourceProjector);
        when(resourceProjector.project("svc")).thenReturn(true);
        when(sender.send(any())).thenReturn(CommonSendResult.builder().brokerStatus("TIMEOUT").build());

        deliver(event, sender);

        assertThat(event.getStatus()).isEqualTo(AuthzOutboxStatus.PENDING);
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getNextRetryAt()).isNotNull();
        assertThat(event.getLastError()).contains("Broker 未确认").contains("TIMEOUT");
    }

    @Test
    @DisplayName("发送返回 null：按未确认处理")
    void shouldRetryWhenResultIsNull() {
        AuthzOutboxEvent event = event(1L, RESOURCE_TOPIC, "svc");
        when(resourceProvider.getIfAvailable()).thenReturn(resourceProjector);
        when(resourceProjector.project("svc")).thenReturn(true);
        when(sender.send(any())).thenReturn(null);

        deliver(event, sender);

        assertThat(event.getStatus()).isEqualTo(AuthzOutboxStatus.PENDING);
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getLastError()).isEqualTo("Broker 未确认: null");
    }

    @Test
    @DisplayName("发送抛异常：记录异常摘要并退避")
    void shouldRetryWhenSendThrows() {
        AuthzOutboxEvent event = event(1L, RESOURCE_TOPIC, "svc");
        when(resourceProvider.getIfAvailable()).thenReturn(resourceProjector);
        when(resourceProjector.project("svc")).thenReturn(true);
        when(sender.send(any())).thenThrow(new IllegalStateException("broker down"));

        deliver(event, sender);

        assertThat(event.getStatus()).isEqualTo(AuthzOutboxStatus.PENDING);
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getNextRetryAt()).isNotNull();
        assertThat(event.getLastError()).isEqualTo("IllegalStateException: broker down");
    }

    @Test
    @DisplayName("异常无消息：只记录异常类型")
    void shouldDescribeExceptionWithoutMessage() {
        AuthzOutboxEvent event = event(1L, RESOURCE_TOPIC, "svc");
        when(resourceProvider.getIfAvailable()).thenReturn(resourceProjector);
        when(resourceProjector.project("svc")).thenReturn(true);
        when(sender.send(any())).thenThrow(new IllegalStateException());

        deliver(event, sender);

        assertThat(event.getLastError()).isEqualTo("IllegalStateException");
    }

    @Test
    @DisplayName("超长异常消息：lastError 截断到 1000 字符")
    void shouldTruncateLongError() {
        AuthzOutboxEvent event = event(1L, RESOURCE_TOPIC, "svc");
        when(resourceProvider.getIfAvailable()).thenReturn(resourceProjector);
        when(resourceProjector.project("svc")).thenReturn(true);
        when(sender.send(any())).thenThrow(new IllegalStateException("x".repeat(1500)));

        deliver(event, sender);

        assertThat(event.getLastError()).hasSize(1000);
    }

    @Test
    @DisplayName("达到最大尝试次数：置 FAILED 终态，不再安排下一次")
    void shouldFailAfterMaxAttempts() {
        properties.getOutbox().setMaxAttempts(10);
        AuthzOutboxEvent event = event(1L, RESOURCE_TOPIC, "svc");
        event.setAttempts(9);
        when(resourceProvider.getIfAvailable()).thenReturn(resourceProjector);
        when(resourceProjector.project("svc")).thenReturn(true);
        when(sender.send(any())).thenReturn(null);

        deliver(event, sender);

        assertThat(event.getAttempts()).isEqualTo(10);
        assertThat(event.getStatus()).isEqualTo(AuthzOutboxStatus.FAILED);
        assertThat(event.getNextRetryAt()).isNull();
    }

    @Test
    @DisplayName("maxAttempts 配置为 0 时按 1 处理")
    void shouldClampMaxAttemptsToAtLeastOne() {
        properties.getOutbox().setMaxAttempts(0);
        AuthzOutboxEvent event = event(1L, RESOURCE_TOPIC, "svc");
        when(resourceProvider.getIfAvailable()).thenReturn(resourceProjector);
        when(resourceProjector.project("svc")).thenReturn(true);
        when(sender.send(any())).thenReturn(null);

        deliver(event, sender);

        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo(AuthzOutboxStatus.FAILED);
    }

    @Test
    @DisplayName("未达上限：保留 PENDING 并设置下一次重试时间")
    void shouldScheduleRetryBeforeMaxAttempts() {
        properties.getOutbox().setMaxAttempts(10);
        AuthzOutboxEvent event = event(1L, RESOURCE_TOPIC, "svc");
        event.setAttempts(1);
        when(resourceProvider.getIfAvailable()).thenReturn(resourceProjector);
        when(resourceProjector.project("svc")).thenReturn(true);
        when(sender.send(any())).thenReturn(null);

        deliver(event, sender);

        assertThat(event.getAttempts()).isEqualTo(2);
        assertThat(event.getStatus()).isEqualTo(AuthzOutboxStatus.PENDING);
        assertThat(event.getNextRetryAt()).isNotNull();
    }

    @Test
    @DisplayName("释放租约失败：不影响已完成的投递状态")
    void shouldTolerateLeaseReleaseFailure() {
        AuthzOutboxEvent event = event(1L, RESOURCE_TOPIC, "svc");
        when(resourceProvider.getIfAvailable()).thenReturn(resourceProjector);
        when(resourceProjector.project("svc")).thenReturn(true);
        when(sender.send(any())).thenReturn(ack());
        AtomicBoolean attempted = new AtomicBoolean();
        doAnswer(invocation -> {
            attempted.set(true);
            throw new IllegalStateException("lease");
        }).when(jdbcTemplate).update(anyString(), any(Object[].class));

        assertThatCode(() -> deliver(event, sender)).doesNotThrowAnyException();
        assertThat(attempted).isTrue();
        assertThat(event.getStatus()).isEqualTo(AuthzOutboxStatus.SENT);
    }

    @Test
    @DisplayName("退避计算：指数增长、上下限与溢出保护")
    void shouldComputeBackoffBoundaries() {
        assertThat(backoff(1L)).isEqualTo(1000L);
        assertThat(backoff(3L)).isEqualTo(4000L);
        assertThat(backoff(30L)).isEqualTo(300_000L);

        properties.getOutbox().setInitialBackoffMs(0L);
        properties.getOutbox().setMaxBackoffMs(0L);
        assertThat(backoff(1L)).isEqualTo(1L);

        properties.getOutbox().setInitialBackoffMs(1000L);
        properties.getOutbox().setMaxBackoffMs(500L);
        assertThat(backoff(2L)).isEqualTo(1000L);

        properties.getOutbox().setInitialBackoffMs(1L);
        properties.getOutbox().setMaxBackoffMs(Long.MAX_VALUE);
        assertThat(backoff(30L)).isEqualTo(1L << 20);

        properties.getOutbox().setInitialBackoffMs(Long.MAX_VALUE);
        properties.getOutbox().setMaxBackoffMs(Long.MAX_VALUE);
        assertThat(backoff(2L)).isEqualTo(Long.MAX_VALUE);
    }

    private long backoff(long attempts) {
        return ReflectionTestUtils.invokeMethod(publisher, "backoffMillis", (int) attempts);
    }

    private long countVarargsUpdates(int varargCount) {
        return mockingDetails(jdbcTemplate).getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals("update"))
                .filter(invocation -> invocation.getArguments().length > 0)
                .filter(invocation -> invocation.getArguments()[0] instanceof String)
                .filter(invocation -> {
                    Object[] args = invocation.getArguments();
                    int count = args.length - 1;
                    if (args.length == 2 && args[1] instanceof Object[] packed) {
                        count = packed.length;
                    }
                    return count == varargCount;
                })
                .count();
    }

    @Test
    @DisplayName("释放空租约集合：不执行 SQL")
    void shouldIgnoreEmptyLeaseRelease() {
        ReflectionTestUtils.invokeMethod(publisher, "releaseLeases", new Object[]{null});
        ReflectionTestUtils.invokeMethod(publisher, "releaseLeases", List.of());

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("批量大小小于 1：SQL limit 钳制为 1")
    void shouldClampBatchSize() {
        properties.getOutbox().setBatchSize(0);
        stubClaim(List.of());

        publisher.poll();

        verify(jdbcTemplate).queryForList(anyString(), eq(Long.class), any(), any(), any(), any(), eq(1));
    }

    @Test
    @DisplayName("批量大小可配置：SQL limit 使用配置值")
    void shouldUseConfiguredBatchSize() {
        properties.getOutbox().setBatchSize(5);
        stubClaim(List.of());

        publisher.poll();

        verify(jdbcTemplate).queryForList(anyString(), eq(Long.class), any(), any(), any(), any(), eq(5));
    }

    @Test
    @DisplayName("publishNow：id 为空或总闸关闭时直接返回")
    void publishNowShouldSkipInvalidInvocation() {
        publisher.publishNow(null);
        properties.setEnabled(false);
        publisher.publishNow(1L);

        verifyNoInteractions(jdbcTemplate, repository);
    }

    @Test
    @DisplayName("publishNow：执行器已关闭时拒绝提交，轮询兜底")
    void publishNowShouldTolerateRejectedExecution() {
        publisher.destroy();

        assertThatCode(() -> publisher.publishNow(1L)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("publishNow：异步领取并投递单条事件")
    void publishNowShouldDeliverAsynchronously() {
        AuthzOutboxEvent event = event(1L, RESOURCE_TOPIC, "svc");
        stubClaimUpdate(1);
        when(senderProvider.getIfAvailable()).thenReturn(sender);
        when(repository.findById(1L)).thenReturn(Optional.of(event));
        when(resourceProvider.getIfAvailable()).thenReturn(resourceProjector);
        when(resourceProjector.project("svc")).thenReturn(true);
        when(sender.send(any())).thenReturn(ack());

        publisher.publishNow(1L);

        verify(repository, timeout(2000)).save(event);
        verify(sender, timeout(2000)).send(any());
        assertThat(event.getStatus()).isEqualTo(AuthzOutboxStatus.SENT);
    }

    @Test
    @DisplayName("立即投递时 Sender 缺失：不领取直接返回")
    void doPublishNowShouldSkipWhenSenderMissing() {
        when(senderProvider.getIfAvailable()).thenReturn(null);

        ReflectionTestUtils.invokeMethod(publisher, "doPublishNow", 1L);

        verify(repository, never()).findById(any());
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("立即投递时领取失败（已被其它实例持有）：不读取事件")
    void doPublishNowShouldSkipWhenClaimRejected() {
        stubClaimUpdate(0);
        when(senderProvider.getIfAvailable()).thenReturn(sender);

        ReflectionTestUtils.invokeMethod(publisher, "doPublishNow", 1L);

        verify(repository, never()).findById(any());
        verify(sender, never()).send(any());
    }

    @Test
    @DisplayName("立即投递时事件已不存在：不发送")
    void doPublishNowShouldSkipWhenEventMissing() {
        stubClaimUpdate(1);
        when(senderProvider.getIfAvailable()).thenReturn(sender);
        when(repository.findById(1L)).thenReturn(Optional.empty());

        ReflectionTestUtils.invokeMethod(publisher, "doPublishNow", 1L);

        verify(sender, never()).send(any());
    }

    @Test
    @DisplayName("立即投递读取事件异常：吞掉，交由轮询重试")
    void doPublishNowShouldSwallowRepositoryFailure() {
        stubClaimUpdate(1);
        when(senderProvider.getIfAvailable()).thenReturn(sender);
        when(repository.findById(1L)).thenThrow(new IllegalStateException("db down"));

        assertThatCode(() -> ReflectionTestUtils.invokeMethod(publisher, "doPublishNow", 1L))
                .doesNotThrowAnyException();
    }
}
