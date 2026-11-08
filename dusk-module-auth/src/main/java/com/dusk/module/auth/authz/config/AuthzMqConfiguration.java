package com.dusk.module.auth.authz.config;

import com.dusk.common.mqs.core.MessageSender;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import tools.jackson.databind.ObjectMapper;

/**
 * 权限事件推送（MQ）装配，见《权限优化方案-整理版》8.5 / 8.6。
 *
 * <p><b>总闸</b>：{@code app.authz.mq.enabled}（默认 {@code false}，8.11「默认全关，逐级打开」）。
 * 它与 {@code dusk.mqs.rocketmq.enabled} 是<b>两个独立开关</b>：RocketMQ 组件由 mqs 的
 * {@code RocketMQAutoConfiguration} 统一装配，本类只负责引入 Outbox 发布器。
 * 只打开本开关、未打开 {@code dusk.mqs.rocketmq.enabled} 时，{@code MessageSender} 不存在，
 * 发布器会以「限流 WARN + 事件保留 PENDING」的方式退化，不会丢事件。</p>
 *
 * <p><b>{@code @EnableScheduling}</b>：Outbox 的兜底轮询依赖它。放在本配置上而非启动类，
 * 是为了让「关闭事件推送」时连调度线程都不启动。</p>
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(prefix = "app.authz.mq", name = "enabled", havingValue = "true")
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