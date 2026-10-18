package com.dusk.module.auth.authz.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 权限事件推送（MQ）配置，前缀 {@code app.authz.mq}。见《权限优化方案-整理版》8.5 / 8.9。
 *
 * <p><b>主题名默认值即是跨仓契约</b>：消费端在 Gateway（{@code app.authorization.mq.*}），
 * 两侧默认值必须逐字一致（8.2）。改名等价于静默断链，因此这里的默认值不随环境变化。</p>
 *
 * <p>本配置与 {@code dusk.mq.rocketmq.enabled} 是<b>两个独立开关</b>：前者控制「本模块是否
 * 生产/消费事件」，后者控制「mqs 通用层是否装配」。只有两者都打开，事件才真正流动（8.3 实测约束）。</p>
 *
 * <p><b>为什么用 {@code @Component} 而不是 {@code @EnableConfigurationProperties}</b>：本类需要
 * 在事件推送<b>关闭</b>时依然存在——{@link AuthzEventPublisher} 无条件装配并依赖它来判断是否跳过。
 * 若只在 {@code AuthzMqConfiguration} 上声明 {@code @EnableConfigurationProperties}，
 * 关闭总闸时该配置类不生效，属性 bean 缺失会让依赖它的服务启动失败。
 * 与 mqs 的 {@code RocketMQProperties} 采用同一写法。</p>
 */
@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "app.authz.mq")
public class AuthzMqProperties {

    /**
     * 本模块事件生产/消费总闸，<b>默认关闭</b>（8.11「默认全关，逐级打开」）。
     *
     * <p>关闭后：不写 Outbox、不投递、不装配消费者。业务侧调用发布入口退化为 no-op，
     * 绝不会影响业务事务。</p>
     */
    private boolean enabled = false;

    /**
     * 业务域，仅用于默认消费组/主题命名（对齐 mqs 的 {@code getDomain()} 语义）。
     */
    private String domain = "authz";

    /**
     * 资源定义变更事件主题（Auth → Gateway）。消费端默认值同此，见 {@code AuthorizationProperties.Mq}。
     */
    private String resourceChangedTopic = "RESOURCE_CHANGED";

    /**
     * 角色权限变更事件主题（Auth → Gateway）。
     */
    private String permissionChangedTopic = "PERMISSION_CHANGED";

    /**
     * 资源快照增量注册主题（SDK → Auth）。
     */
    private String snapshotRegisterTopic = "RESOURCE_SNAPSHOT_REGISTERED";

    /**
     * 投递前是否先刷新 Redis 读模型（3.8 第 1 点）。
     *
     * <p>开启后，{@code RESOURCE_CHANGED} 会在发出 MQ 之前先执行
     * {@code ResourceReadModelProjector.project(serviceId)}，保证网关收到失效事件时 L2 已是新值，
     * 从而避免「失效 → 回源」读到旧快照。</p>
     */
    private boolean refreshReadModelBeforePublish = true;

    private Outbox outbox = new Outbox();

    /**
     * Transactional Outbox 参数（8.5）。
     */
    @Getter
    @Setter
    public static class Outbox {

        /**
         * 是否启用 Outbox。关闭后事件将<b>不落库、不投递</b>（等价于关闭事件推送，但业务不受影响）。
         */
        private boolean enabled = true;

        /**
         * 兜底轮询间隔（毫秒）。正常路径由事务提交后的立即唤醒投递，本轮询用于覆盖
         * 「进程重启 / 唤醒回调丢失 / 投递瞬时失败」。
         */
        private long pollIntervalMs = 1000L;

        /**
         * 单批投递上限，避免一次轮询占用过多时间。
         */
        private int batchSize = 200;

        /**
         * 单条事件最大投递尝试次数，超过后置 {@code FAILED} 并告警（人工介入，不丢弃）。
         */
        private int maxAttempts = 10;

        /**
         * 首次失败后的退避基数（毫秒）；后续按 {@code base * 2^(attempts-1)} 指数退避，上限见 {@link #maxBackoffMs}。
         */
        private long initialBackoffMs = 1000L;

        /**
         * 退避上限（毫秒）。
         */
        private long maxBackoffMs = 300000L;
    }
}
