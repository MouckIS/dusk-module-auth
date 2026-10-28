package com.dusk.module.auth.authz.config;

import com.dusk.common.mqs.core.InterceptorChain;
import com.dusk.common.mqs.core.MessageSender;
import com.dusk.common.mqs.rocketmq.config.RocketMQProperties;
import com.dusk.common.mqs.rocketmq.consumer.RocketMQConsumerRegistry;
import com.dusk.common.mqs.rocketmq.idempotency.DefaultIdempotencyService;
import com.dusk.common.mqs.rocketmq.idempotency.RocketMQIdempotencyInterceptor;
import com.dusk.common.mqs.rocketmq.interceptor.MetricsInterceptor;
import com.dusk.common.mqs.rocketmq.interceptor.TraceLoggingInterceptor;
import com.dusk.common.mqs.rocketmq.producer.RocketMQMessageSender;
import com.dusk.common.mqs.rocketmq.threadpool.MqDynamicThreadPoolManager;
import com.dusk.module.auth.authz.outbox.AuthzOutboxPublisher;
import com.dusk.module.auth.authz.outbox.IAuthzOutboxEventRepository;
import com.dusk.module.auth.authz.readmodel.RolePermissionReadModelProjector;
import com.dusk.module.auth.authz.readmodel.SubjectReadModelProjector;
import com.dusk.module.auth.registry.service.ResourceReadModelProjector;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import tools.jackson.databind.ObjectMapper;

/**
 * 权限事件推送（MQ）装配，见《权限优化方案-整理版》8.5 / 8.6。
 *
 * <p><b>总闸</b>：{@code app.authz.mq.enabled}（默认 {@code false}，8.11「默认全关，逐级打开」）。
 * 注意它与 {@code dusk.mq.rocketmq.enabled} 是<b>两个独立开关</b>：本类负责把 mqs 的组件引入容器，
 * 而各组件自身带 {@code @Conditional(RocketMQEnableCondition.class)}，是否真正生效由后者决定。
 * 只打开本开关、未打开 {@code dusk.mq.rocketmq.enabled} 时，{@code MessageSender} 不存在，
 * 发布器会以「限流 WARN + 事件保留 PENDING」的方式退化，不会丢事件。</p>
 *
 * <p><b>为什么显式 {@code @Import} 而不是放开组件扫描</b>：Auth 的启动类
 * {@code scanBasePackages} 只覆盖 {@code com.dusk.module.auth / com.dusk.common.core / com.dusk.common.rpc}，
 * <b>不含</b> {@code com.dusk.common.mqs}；而 mqs 没有 {@code AutoConfiguration.imports}，
 * 只能靠扫描或显式导入装配。整包扫描 mqs 会连带引入 MQTT / 短信推送等无关配置类，
 * 因此这里只导入本链路必需的那几个类（与网关侧 {@code GatewayAuthorizationConfiguration.MqConfiguration} 同一策略）。</p>
 *
 * <p>被导入的类大多自带 {@code @Conditional(RocketMQEnableCondition.class)}，因此重复导入安全；
 * {@code InterceptorChain} 是无条件组件，导入后即使 MQ 未启用也只是个空链。</p>
 *
 * <p><b>{@code @EnableScheduling}</b>：Outbox 的兜底轮询依赖它。放在本配置上而非启动类，
 * 是为了让「关闭事件推送」时连调度线程都不启动。</p>
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(prefix = "app.authz.mq", name = "enabled", havingValue = "true")
@Import({RocketMQProperties.class,
        InterceptorChain.class,
        MqDynamicThreadPoolManager.class,
        DefaultIdempotencyService.class,
        RocketMQIdempotencyInterceptor.class,
        TraceLoggingInterceptor.class,
        MetricsInterceptor.class,
        RocketMQMessageSender.class,
        RocketMQConsumerRegistry.class})
public class AuthzMqConfiguration {

    /**
     * Outbox 发布器（6.2 / 6.3）。依赖 {@code ObjectProvider} 延迟取 {@code MessageSender}，
     * 使「mqs 未启用 / broker 不可达」时本 bean 仍可创建，退化为「保留事件 + 限流告警」。
     */
    @Bean
    @ConditionalOnMissingBean
    public AuthzOutboxPublisher authzOutboxPublisher(IAuthzOutboxEventRepository repository,
                                                     AuthzMqProperties properties,
                                                     ObjectProvider<MessageSender> senderProvider,
                                                     ObjectProvider<ResourceReadModelProjector> resourceProjectorProvider,
                                                     ObjectProvider<RolePermissionReadModelProjector> roleProjectorProvider,
                                                     ObjectProvider<SubjectReadModelProjector> subjectProjectorProvider,
                                                     ObjectMapper objectMapper,
                                                     JdbcTemplate jdbcTemplate) {
        return new AuthzOutboxPublisher(repository, properties, senderProvider,
                resourceProjectorProvider, roleProjectorProvider, subjectProjectorProvider,
                objectMapper, jdbcTemplate);
    }
}
