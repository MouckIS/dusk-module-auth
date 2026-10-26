package com.dusk.module.auth.authz.service;

import com.dusk.common.core.auth.registry.AuthzScope;
import com.dusk.module.auth.authz.config.AuthzMqProperties;
import com.dusk.module.auth.authz.event.PermissionChangedEvent;
import com.dusk.module.auth.authz.event.ResourceChangedEvent;
import com.dusk.module.auth.authz.event.SubjectChangedEvent;
import com.dusk.module.auth.authz.outbox.AuthzOutboxEvent;
import com.dusk.module.auth.authz.outbox.AuthzOutboxProjectionStatus;
import com.dusk.module.auth.authz.outbox.AuthzOutboxPublisher;
import com.dusk.module.auth.authz.outbox.IAuthzOutboxEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.ObjectMapper;

/**
 * 事件发布入口（业务侧唯一依赖），见《权限优化方案-整理版》8.5。
 *
 * <p>职责是「<b>在业务事务内</b>把事件写入 Outbox，并在事务提交后唤醒发布器」。
 * 真正的投递由 {@link AuthzOutboxPublisher} 完成——业务代码不直面 MQ，也不感知投递失败。</p>
 *
 * <p><b>为什么写入必须发生在业务事务内</b>：Outbox 模式的核心是「业务事实变更」与「事件产生」
 * 同一事务提交。若事件写在一个独立事务里，一旦业务回滚，事件却已产生（幽灵事件）；
 * 反之若先发 MQ 再写库，库失败则事件已生效（无法撤回）。因此本类的每个方法都<b>假定调用方已开启事务</b>。</p>
 *
 * <p><b>失败语义（刻意不吞异常）</b>：Outbox 写入失败会让整个业务事务回滚——这是有意为之。
 * 「权限事实变了但事件写不进去」正是 Outbox 要消灭的不一致状态。若希望事件成为「尽力而为」，
 * 应在调用方显式选择（不要在本类内部静默降级）。</p>
 */
@Service
@Slf4j
public class AuthzEventPublisher {

    private final IAuthzOutboxEventRepository repository;
    private final AuthzMqProperties properties;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<AuthzOutboxPublisher> publisherProvider;

    public AuthzEventPublisher(IAuthzOutboxEventRepository repository,
                               AuthzMqProperties properties,
                               ObjectMapper objectMapper,
                               ObjectProvider<AuthzOutboxPublisher> publisherProvider) {
        this.repository = repository;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.publisherProvider = publisherProvider;
    }

    /**
     * 发布「资源定义变更」事件（→ Gateway，失效 routePermCache）。
     *
     * @param serviceId       服务标识
     * @param resourceVersion 变更后的内容哈希
     * @param releaseVersion  产出该版本的 Release 版本号
     */
    public void resourceChanged(String serviceId, String resourceVersion, String releaseVersion) {
        resourceChanged(serviceId, resourceVersion, releaseVersion, 0L);
    }

    /**
     * 发布「资源定义变更」事件（→ Gateway，失效 routePermCache）。
     *
     * @param serviceId        服务标识
     * @param resourceVersion  变更后的内容哈希
     * @param releaseVersion   产出该版本的 Release 版本号
     * @param resourceRevision 变更后的服务级修订号（单调递增）
     */
    public void resourceChanged(String serviceId, String resourceVersion, String releaseVersion,
                                long resourceRevision) {
        if (serviceId == null || serviceId.isBlank() || resourceVersion == null || resourceVersion.isBlank()) {
            log.debug("跳过 RESOURCE_CHANGED：serviceId 或 resourceVersion 为空（serviceId={}, resourceVersion={}）",
                    serviceId, resourceVersion);
            return;
        }
        ResourceChangedEvent event = ResourceChangedEvent.of(serviceId, resourceVersion, releaseVersion, resourceRevision);
        append(properties.getResourceChangedTopic(), event.bizKey(), event.refKey(), event, resourceRevision);
    }

    /**
     * 发布「角色权限变更」事件（→ Gateway，失效 rolePermCache）。
     *
     * @param tenantId          租户 ID，Host 为 {@code null}
     * @param roleId            角色 ID
     * @param permissionVersion 递增后的新版本号
     */
    public void permissionChanged(Long tenantId, Long roleId, long permissionVersion) {
        permissionChanged(AuthzScope.of(tenantId), tenantId, roleId, permissionVersion);
    }

    /**
     * 发布「角色权限变更」事件（带显式作用域，避免 Host 被 null 过滤）。
     */
    public void permissionChanged(String tenantScope, Long tenantId, Long roleId, long permissionVersion) {
        if (tenantScope == null || tenantScope.isBlank() || roleId == null) {
            log.debug("跳过 PERMISSION_CHANGED：tenantScope 或 roleId 为空（tenantScope={}, roleId={}）",
                    tenantScope, roleId);
            return;
        }
        PermissionChangedEvent event = PermissionChangedEvent.of(tenantScope, tenantId, roleId, permissionVersion);
        append(properties.getPermissionChangedTopic(), event.bizKey(), event.refKey(), event, permissionVersion);
    }

    /**
     * 发布「账号状态/用户代际变更」事件（→ Gateway，失效 subjectCache）。
     */
    public void subjectChanged(String tenantScope, Long userId, long subjectVersion, boolean enabled) {
        if (tenantScope == null || tenantScope.isBlank() || userId == null) {
            log.debug("跳过 SUBJECT_CHANGED：tenantScope 或 userId 为空（tenantScope={}, userId={}）",
                    tenantScope, userId);
            return;
        }
        SubjectChangedEvent event = SubjectChangedEvent.of(tenantScope, userId, subjectVersion, enabled);
        append(properties.getSubjectChangedTopic(), event.bizKey(), event.refKey(), event, subjectVersion);
    }

    private void append(String eventType, String bizKey, String refKey, Object payload, long revision) {
        if (!properties.isEnabled() || !properties.getOutbox().isEnabled()) {
            log.debug("事件推送未启用，跳过 Outbox 写入: eventType={}, bizKey={}", eventType, bizKey);
            return;
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            // 载荷是本地 record，序列化失败属编程错误：抛出以便测试暴露，而不是静默产生一个坏事件。
            throw new IllegalStateException("事件载荷序列化失败: eventType=" + eventType + ", bizKey=" + bizKey, e);
        }

        AuthzOutboxEvent row = new AuthzOutboxEvent();
        row.setEventType(eventType);
        row.setBizKey(bizKey);
        row.setRefKey(refKey);
        row.setPayload(json);
        row.setRevision(revision);
        row.setProjectionStatus(AuthzOutboxProjectionStatus.PENDING);
        repository.save(row);
        log.debug("Outbox 事件已写入: id={}, eventType={}, bizKey={}", row.getId(), eventType, bizKey);

        Long outboxId = row.getId();
        registerAfterCommitWakeup(outboxId);
    }

    /**
     * 事务提交后唤醒发布器（低延迟）；无事务时直接唤醒（例如单元测试或非事务调用点）。
     */
    private void registerAfterCommitWakeup(Long outboxId) {
        if (outboxId == null) {
            return;
        }
        Runnable wakeup = () -> {
            AuthzOutboxPublisher publisher = publisherProvider.getIfAvailable();
            if (publisher != null) {
                publisher.publishNow(outboxId);
            }
        };
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            wakeup.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                wakeup.run();
            }
        });
    }
}
