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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import tools.jackson.databind.ObjectMapper;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Outbox 发布器：把 {@link AuthzOutboxEvent} 投影到 Redis 后投递到 RocketMQ，
 * 见《权限优化方案-整理版》6.2 / 6.3。
 *
 * <p><b>多实例安全</b>：领取使用 PostgreSQL {@code FOR UPDATE SKIP LOCKED} + 租约，
 * 不依赖 JVM 内锁；崩溃后租约到期可恢复，避免任务永久卡在 PROCESSING。</p>
 *
 * <p><b>投影与投递分离</b>：投影失败只记录投影状态并退避重试，<b>不得发送事件</b>；
 * 只有投影成功才允许发送。发送成功后才记录 {@code sent_at}；发送后崩溃导致的重复投递
 * 是允许的（至少一次），消费者必须幂等。</p>
 */
@Slf4j
public class AuthzOutboxPublisher implements DisposableBean {

    private static final String SEND_OK_PREFIX = "SEND_OK";
    private static final String REJECTED = "REJECTED";
    private static final long UNAVAILABLE_WARN_INTERVAL_MS = 60_000L;

    private final IAuthzOutboxEventRepository repository;
    private final AuthzMqProperties properties;
    private final ObjectProvider<MessageSender> senderProvider;
    private final ObjectProvider<ResourceReadModelProjector> resourceProjectorProvider;
    private final ObjectProvider<RolePermissionReadModelProjector> roleProjectorProvider;
    private final ObjectProvider<SubjectReadModelProjector> subjectProjectorProvider;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    private final AtomicLong lastUnavailableWarnAt = new AtomicLong(0L);
    private final String leaseOwner = buildLeaseOwner();

    /**
     * 立即投递的专用执行器：单线程、守护线程，避免阻塞业务线程。
     */
    private final ExecutorService wakeupExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "authz-outbox-immediate");
        thread.setDaemon(true);
        return thread;
    });

    public AuthzOutboxPublisher(IAuthzOutboxEventRepository repository,
                                AuthzMqProperties properties,
                                ObjectProvider<MessageSender> senderProvider,
                                ObjectProvider<ResourceReadModelProjector> resourceProjectorProvider,
                                ObjectProvider<RolePermissionReadModelProjector> roleProjectorProvider,
                                ObjectProvider<SubjectReadModelProjector> subjectProjectorProvider,
                                ObjectMapper objectMapper,
                                JdbcTemplate jdbcTemplate) {
        this.repository = repository;
        this.properties = properties;
        this.senderProvider = senderProvider;
        this.resourceProjectorProvider = resourceProjectorProvider;
        this.roleProjectorProvider = roleProjectorProvider;
        this.subjectProjectorProvider = subjectProjectorProvider;
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    private static String brokerStatus(CommonSendResult result) {
        return result == null ? null : result.getBrokerStatus();
    }

    private static Long parseLong(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String buildLeaseOwner() {
        String host;
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            host = "unknown";
        }
        String jvm = ManagementFactory.getRuntimeMXBean().getName();
        return host + ":" + jvm + ":" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Override
    public void destroy() {
        wakeupExecutor.shutdown();
        log.info("Outbox 立即投递执行器已关闭");
    }

    /**
     * 兜底轮询：领取到期事件并投递。
     */
    @Scheduled(fixedDelayString = "${app.authz.mq.outbox.poll-interval-ms:1000}")
    public void poll() {
        if (!active()) {
            return;
        }
        try {
            List<Long> claimed = claimDue();
            if (claimed.isEmpty()) {
                return;
            }
            MessageSender sender = senderProvider.getIfAvailable();
            if (sender == null) {
                warnSenderUnavailable();
                releaseLeases(claimed);
                return;
            }
            for (AuthzOutboxEvent event : load(claimed)) {
                deliver(event, sender);
            }
        } catch (Exception e) {
            log.error("Outbox 轮询发生异常，下一轮继续", e);
        }
    }

    /**
     * 事务提交后的立即投递（低延迟通道）。
     */
    public void publishNow(Long outboxId) {
        if (outboxId == null || !active()) {
            return;
        }
        try {
            wakeupExecutor.execute(() -> doPublishNow(outboxId));
        } catch (RejectedExecutionException e) {
            log.debug("Outbox 立即投递被拒绝（执行器已关闭），交由轮询兜底: id={}", outboxId);
        }
    }

    private void doPublishNow(Long outboxId) {
        MessageSender sender = senderProvider.getIfAvailable();
        if (sender == null) {
            warnSenderUnavailable();
            return;
        }
        try {
            if (!claimById(outboxId)) {
                return;
            }
            repository.findById(outboxId).ifPresent(event -> deliver(event, sender));
        } catch (Exception e) {
            log.warn("Outbox 立即投递失败（将由轮询兜底重试）: id={}", outboxId, e);
        }
    }

    /**
     * 单条处理。调用方必须已持有该行的租约。
     */
    private void deliver(AuthzOutboxEvent event, MessageSender sender) {
        try {
            if (!project(event)) {
                markProjectionFailed(event, "读模型投影失败");
                return;
            }
            event.setProjectionStatus(AuthzOutboxProjectionStatus.PROJECTED);
            event.setProjectedAt(LocalDateTime.now());

            MqMessage<Object> message = MqMessage.<Object>builder()
                    .topic(event.getEventType())
                    .payload(deserialize(event))
                    .bizKey(event.getBizKey())
                    .keys(event.getBizKey())
                    .build();

            CommonSendResult result = sender.send(message);
            if (isAcknowledged(result)) {
                markSent(event);
            } else if (isRejected(result)) {
                markSendFailedTerminal(event, "Broker 拒绝: " + brokerStatus(result));
            } else {
                markSendFailed(event, "Broker 未确认: " + brokerStatus(result));
            }
        } catch (Exception e) {
            markSendFailed(event, describe(e));
        } finally {
            clearLease(event);
        }
    }

    /**
     * 投影：先写 Redis 读模型，成功后才允许发送事件。
     */
    private boolean project(AuthzOutboxEvent event) {
        String type = event.getEventType();
        if (properties.getResourceChangedTopic().equals(type)) {
            ResourceReadModelProjector projector = resourceProjectorProvider.getIfAvailable();
            return projector != null && projector.project(event.getRefKey());
        }
        if (properties.getPermissionChangedTopic().equals(type)) {
            RolePermissionReadModelProjector projector = roleProjectorProvider.getIfAvailable();
            if (projector == null || event.getRefKey() == null) {
                return false;
            }
            int split = event.getRefKey().lastIndexOf(':');
            if (split <= 0) {
                return false;
            }
            String scope = event.getRefKey().substring(0, split);
            Long roleId = parseLong(event.getRefKey().substring(split + 1));
            return roleId != null && projector.project(scope, roleId);
        }
        if (properties.getSubjectChangedTopic().equals(type)) {
            SubjectReadModelProjector projector = subjectProjectorProvider.getIfAvailable();
            if (projector == null || event.getRefKey() == null) {
                return false;
            }
            int split = event.getRefKey().lastIndexOf(':');
            if (split <= 0) {
                return false;
            }
            String scope = event.getRefKey().substring(0, split);
            Long userId = parseLong(event.getRefKey().substring(split + 1));
            return userId != null && projector.project(scope, userId);
        }
        // 未知类型没有投影器：显式跳过，避免整条链路因未来事件类型停摆。
        event.setProjectionStatus(AuthzOutboxProjectionStatus.SKIPPED);
        return true;
    }

    private Object deserialize(AuthzOutboxEvent event) {
        String type = event.getEventType();
        if (properties.getResourceChangedTopic().equals(type)) {
            return objectMapper.readValue(event.getPayload(), ResourceChangedEvent.class);
        }
        if (properties.getPermissionChangedTopic().equals(type)) {
            return objectMapper.readValue(event.getPayload(), PermissionChangedEvent.class);
        }
        if (properties.getSubjectChangedTopic().equals(type)) {
            return objectMapper.readValue(event.getPayload(), SubjectChangedEvent.class);
        }
        log.warn("Outbox 遇到未知事件类型，按原始文本投递: id={}, eventType={}", event.getId(), type);
        return event.getPayload();
    }

    private void markSent(AuthzOutboxEvent event) {
        event.setStatus(AuthzOutboxStatus.SENT);
        event.setSentAt(LocalDateTime.now());
        event.setLastError(null);
        repository.save(event);
        log.info("Outbox 事件已投递: id={}, eventType={}, bizKey={}, revision={}, attempts={}",
                event.getId(), event.getEventType(), event.getBizKey(), event.getRevision(), event.getAttempts());
    }

    private void markProjectionFailed(AuthzOutboxEvent event, String error) {
        event.setProjectionStatus(AuthzOutboxProjectionStatus.FAILED);
        event.setLastError(truncate(error));
        scheduleRetry(event);
        log.warn("Outbox 投影失败，事件保持未发送并退避重试: id={}, eventType={}, refKey={}, error={}",
                event.getId(), event.getEventType(), event.getRefKey(), error);
    }

    private void markSendFailed(AuthzOutboxEvent event, String error) {
        event.setLastError(truncate(error));
        scheduleRetry(event);
        log.warn("Outbox 事件投递失败，将退避重试: id={}, eventType={}, attempts={}, error={}",
                event.getId(), event.getEventType(), event.getAttempts(), error);
    }

    /**
     * Broker 明确拒绝是终态，不进入自动重试（6.3）。
     */
    private void markSendFailedTerminal(AuthzOutboxEvent event, String error) {
        event.setStatus(AuthzOutboxStatus.FAILED);
        event.setLastError(truncate(error));
        repository.save(event);
        log.error("Outbox 事件被 Broker 拒绝，置为 FAILED 待人工重放: id={}, eventType={}, error={}",
                event.getId(), event.getEventType(), error);
    }

    private void scheduleRetry(AuthzOutboxEvent event) {
        int attempts = event.getAttempts() + 1;
        event.setAttempts(attempts);
        if (attempts >= Math.max(1, properties.getOutbox().getMaxAttempts())) {
            event.setStatus(AuthzOutboxStatus.FAILED);
        } else {
            event.setNextRetryAt(LocalDateTime.now().plusNanos(backoffMillis(attempts) * 1_000_000L));
        }
        repository.save(event);
    }

    private long backoffMillis(int attempts) {
        long base = Math.max(1L, properties.getOutbox().getInitialBackoffMs());
        long max = Math.max(base, properties.getOutbox().getMaxBackoffMs());
        int shift = Math.min(Math.max(0, attempts - 1), 20);
        long value = base << shift;
        return value <= 0 ? max : Math.min(value, max);
    }

    private boolean isAcknowledged(CommonSendResult result) {
        String status = brokerStatus(result);
        return status != null && status.startsWith(SEND_OK_PREFIX);
    }

    private boolean isRejected(CommonSendResult result) {
        return REJECTED.equals(brokerStatus(result));
    }

    private List<Long> claimDue() {
        int batch = Math.max(1, properties.getOutbox().getBatchSize());
        LocalDateTime now = LocalDateTime.now();
        List<Long> ids = jdbcTemplate.queryForList("""
                update sys_authz_outbox_event
                   set lease_owner = ?, lease_until = ?
                 where id in (
                       select id from sys_authz_outbox_event
                        where status = 'PENDING'
                          and (next_retry_at is null or next_retry_at <= ?)
                          and (lease_until is null or lease_until < ?)
                        order by id
                        limit ?
                          for update skip locked
                 )
                returning id
                """, Long.class, leaseOwner, leaseUntil(), now, now, batch);
        return ids == null ? List.of() : ids;
    }

    private boolean claimById(Long id) {
        LocalDateTime now = LocalDateTime.now();
        int updated = jdbcTemplate.update("""
                update sys_authz_outbox_event
                   set lease_owner = ?, lease_until = ?
                 where id = ?
                   and status = 'PENDING'
                   and (next_retry_at is null or next_retry_at <= ?)
                   and (lease_until is null or lease_until < ?)
                """, leaseOwner, leaseUntil(), id, now, now);
        return updated > 0;
    }

    private void releaseLeases(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            jdbcTemplate.update("update sys_authz_outbox_event set lease_owner = null, lease_until = null where id = ? and lease_owner = ?",
                    id, leaseOwner);
        }
    }

    private void clearLease(AuthzOutboxEvent event) {
        try {
            jdbcTemplate.update("update sys_authz_outbox_event set lease_owner = null, lease_until = null where id = ? and lease_owner = ?",
                    event.getId(), leaseOwner);
        } catch (Exception e) {
            log.debug("释放 Outbox 租约失败（租约到期后会自动恢复）: id={}", event.getId(), e);
        }
    }

    private boolean active() {
        return properties.isEnabled() && properties.getOutbox().isEnabled();
    }

    private void warnSenderUnavailable() {
        long now = System.currentTimeMillis();
        long last = lastUnavailableWarnAt.get();
        if (now - last < UNAVAILABLE_WARN_INTERVAL_MS || !lastUnavailableWarnAt.compareAndSet(last, now)) {
            return;
        }
        log.warn("Outbox 未投递：MessageSender 不可用。请确认 dusk.mq.rocketmq.enabled=true 且 broker 可达；"
                + "事件将持续保留在 sys_authz_outbox_event（status=PENDING），恢复后自动投递。");
    }

    private static String describe(Throwable e) {
        String message = e.getMessage();
        return e.getClass().getSimpleName() + (message == null || message.isBlank() ? "" : ": " + message);
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }

    private List<AuthzOutboxEvent> load(List<Long> ids) {
        List<AuthzOutboxEvent> events = new ArrayList<>(repository.findAllById(ids));
        events.sort(Comparator.comparing(AuthzOutboxEvent::getId));
        return events;
    }

    private LocalDateTime leaseUntil() {
        return LocalDateTime.now().plusSeconds(Math.max(5L, properties.getOutbox().getLeaseSeconds()));
    }
}
