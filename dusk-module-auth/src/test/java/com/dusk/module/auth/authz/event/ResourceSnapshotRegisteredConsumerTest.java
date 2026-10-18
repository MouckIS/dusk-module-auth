package com.dusk.module.auth.authz.event;

import com.dusk.common.core.auth.registry.ResourceSnapshot;
import com.dusk.common.core.auth.registry.SnapshotAnonymousResource;
import com.dusk.common.core.auth.registry.SnapshotResource;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.mqs.core.ConsumeMode;
import com.dusk.common.mqs.core.MqConsumeContext;
import com.dusk.module.auth.authz.config.AuthzMqProperties;
import com.dusk.module.auth.registry.dto.RegistryRegistrationResponse;
import com.dusk.module.auth.registry.service.IResourceRegistryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ResourceSnapshotRegisteredConsumer} 测试，覆盖《权限优化方案-整理版》8.4 / 8.6 的 MQ 注册语义。
 *
 * <p>两条必守的边界：</p>
 * <ol>
 *   <li><b>复用同一业务入口</b>：MQ 通道必须调用 {@link IResourceRegistryService#register}，
 *       而不是另写一套落库逻辑——否则 Diff / 权限生命周期 / 读模型投影会与 HTTP 通道漂移（8.6）；</li>
 *   <li><b>失败分类</b>：确定性校验失败必须确认消费（不重投），可重试故障必须抛出交给 broker。
 *       分类错了会造成两种相反的故障：把可重试错误确认掉 = 静默丢注册；把确定性错误抛出 = 重投到死信。</li>
 * </ol>
 *
 * <p>另外固定跨仓契约的订阅参数：SDK 侧 {@code app.permission.sdk.publish.topic} 必须与
 * {@code app.authz.mq.snapshot-register-topic} 的默认值逐字一致，否则表现为「发布成功但注册不生效」。</p>
 */
class ResourceSnapshotRegisteredConsumerTest {

    private static final String SERVICE_ID = "dusk-module-order";

    private final IResourceRegistryService registryService = mock(IResourceRegistryService.class);

    private AuthzMqProperties properties;
    private ResourceSnapshotRegisteredConsumer consumer;
    private MqConsumeContext context;

    @BeforeEach
    void setUp() {
        properties = new AuthzMqProperties();
        consumer = new ResourceSnapshotRegisteredConsumer(registryService, properties);
        context = new MqConsumeContext();
        context.setMessageId("msg-1");
        context.setHeaders(Map.of("sdk.instanceId", "order@10.0.0.1:8080"));
    }

    private static ResourceSnapshot snapshot() {
        return ResourceSnapshot.of(SERVICE_ID, "1.2.0",
                List.of(SnapshotResource.of("GET", "/orders", "order:query")),
                List.of(SnapshotAnonymousResource.of("POST", "/login")));
    }

    private static RegistryRegistrationResponse response() {
        return new RegistryRegistrationResponse(SERVICE_ID, "1.2.0", "rv-1", false, "REGISTERED", 2,
                List.of("GET /orders"), List.of(), List.of(), List.of(), List.of("order:query"), List.of());
    }

    @Test
    @DisplayName("正常消息：原样把快照交给注册服务（与 HTTP 通道同一入口）")
    void shouldDelegateSnapshotToRegistryService() {
        when(registryService.register(any(ResourceSnapshot.class))).thenReturn(response());

        consumer.process(snapshot(), context);

        ArgumentCaptor<ResourceSnapshot> captor = ArgumentCaptor.forClass(ResourceSnapshot.class);
        verify(registryService).register(captor.capture());
        ResourceSnapshot received = captor.getValue();
        assertThat(received.serviceId()).isEqualTo(SERVICE_ID);
        assertThat(received.serviceVersion()).isEqualTo("1.2.0");
        assertThat(received.resources()).hasSize(1);
        assertThat(received.anonymousResources()).hasSize(1);
    }

    @Test
    @DisplayName("空载荷：记日志后确认，不触碰注册服务")
    void shouldIgnoreNullPayload() {
        assertThatCode(() -> consumer.process(null, context)).doesNotThrowAnyException();

        verify(registryService, never()).register(any());
    }

    @Test
    @DisplayName("消费上下文缺失或没有传输头时仍能正常注册（头只用于日志定位）")
    void shouldTolerateMissingContextAndHeaders() {
        when(registryService.register(any(ResourceSnapshot.class))).thenReturn(response());

        assertThatCode(() -> consumer.process(snapshot(), null)).doesNotThrowAnyException();

        MqConsumeContext withoutHeaders = new MqConsumeContext();
        withoutHeaders.setHeaders(null);
        assertThatCode(() -> consumer.process(snapshot(), withoutHeaders)).doesNotThrowAnyException();

        verify(registryService, org.mockito.Mockito.times(2)).register(any(ResourceSnapshot.class));
    }

    @Test
    @DisplayName("确定性校验失败（BusinessException）：确认不重投，避免刷满死信")
    void shouldAcknowledgeDeterministicRejection() {
        when(registryService.register(any(ResourceSnapshot.class)))
                .thenThrow(new BusinessException("resourceVersion 与资源内容不一致"));

        assertThatCode(() -> consumer.process(snapshot(), context)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("服务端故障：抛出交给 broker 重投（与 HTTP 通道 5xx 同义）")
    void shouldRethrowTransientFailure() {
        when(registryService.register(any(ResourceSnapshot.class)))
                .thenThrow(new IllegalStateException("数据库不可用"));

        assertThatThrownBy(() -> consumer.process(snapshot(), context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("数据库不可用");
    }

    @Test
    @DisplayName("订阅契约：主题默认值与 SDK 侧发布主题一致，消费参数符合 8.6")
    void shouldExposeSubscriptionContract() {
        assertThat(consumer.getTopic()).isEqualTo("RESOURCE_SNAPSHOT_REGISTERED");
        assertThat(consumer.getDomain()).isEqualTo("authz");
        assertThat(consumer.getConsumerGroup()).endsWith(".authz.auth-registry-snapshot");
        assertThat(consumer.getPayloadType()).isEqualTo(ResourceSnapshot.class);
        assertThat(consumer.getConsumeMode()).isEqualTo(ConsumeMode.CONCURRENTLY);
        assertThat(consumer.isIdempotent()).isTrue();
        assertThat(consumer.getConsumeThreads()).isEqualTo(2);
    }

    @Test
    @DisplayName("主题可配置覆盖：跨环境改名时两侧必须同步改，消费者组随之隔离")
    void shouldAllowTopicOverride() {
        properties.setSnapshotRegisterTopic("RESOURCE_SNAPSHOT_REGISTERED_SIT");
        properties.setDomain("authz-sit");

        ResourceSnapshotRegisteredConsumer custom = new ResourceSnapshotRegisteredConsumer(registryService, properties);

        assertThat(custom.getTopic()).isEqualTo("RESOURCE_SNAPSHOT_REGISTERED_SIT");
        assertThat(custom.getConsumerGroup()).endsWith(".authz-sit.auth-registry-snapshot");
    }
}
