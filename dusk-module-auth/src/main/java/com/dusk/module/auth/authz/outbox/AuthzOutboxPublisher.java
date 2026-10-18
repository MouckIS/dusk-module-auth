package com.dusk.module.auth.authz.outbox;

import com.dusk.common.mqs.core.CommonSendResult;
import com.dusk.common.mqs.core.MessageSender;
import com.dusk.common.mqs.core.MqMessage;
import com.dusk.module.auth.authz.config.AuthzMqProperties;
import com.dusk.module.auth.authz.event.PermissionChangedEvent;
import com.dusk.module.auth.authz.event.ResourceChangedEvent;
import com.dusk.module.auth.registry.service.ResourceReadModelProjector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Outbox 发布器：把 {@link AuthzOutboxEvent} 投递到 RocketMQ，见《权限优化方案-整理版》8.5。
 *
 * <p>两条触发路径：</p>
 * <ol>
 *   <li><b>事务提交后立即唤醒</b>（{@link #publishNow(Long)}）——正常路径的低延迟通道；</li>
 *   <li><b>兜底轮询</b>（{@link #poll()}，{@code @Scheduled}）——覆盖进程重启、唤醒回调丢失、
 *       投递瞬时失败等异常路径。</li>
 * </ol>
 * <p>两条路径由同一把锁串行化，因此同一条事件不会被并发重复投递。</p>
 *
 * <p><b>投递前先刷新 Redis 读模型</b>（3.8 第 1 点）：{@code RESOURCE_CHANGED} 会先执行
 * {@code ResourceReadModelProjector.project(serviceId)}，<b>刷新失败则不发送并进入退避重试</b>——
 * 这保证网关收到失效事件时 L2 必已是新值，避免「失效 → 回源读到旧快照」。读模型是可重算的投影，
 * 因此重试是安全且收敛的。</p>
 *
 * <p><b>失败语义</b>：投递失败 {@code attempts++} 并指数退避，超过 {@code maxAttempts} 置
 * {@code FAILED} 并 ERROR 告警——事件<b>绝不静默丢弃</b>，由运维人工重放。</p>
 *
 * <p><b>为什么立即投递走独立线程</b>：{@link #publishNow(Long)} 由业务事务的 {@code afterCommit}
 * 触发，而 {@code MessageSender.send} 是同步阻塞的（{@code send-msg-timeout} 默认 3s）。若在同一
 * 线程内投递，一次 broker 抖动就会把延迟叠加到「快照注册的 HTTP 响应」或「角色保存接口」上。
 * 因此立即投递被提交到一个单线程守护执行器——既不改业务线程的时延，又保持投递顺序
 * （单线程 + {@link #lock} 双重保证）。</p>
 */
@Slf4j
public class AuthzOutboxPublisher implements DisposableBean {

    private static final String SEND_OK_PREFIX = "SEND_OK";
    private static final String REJECTED = "REJECTED";
    private static final long UNAVAILABLE_WARN_INTERVAL_MS = 60_000L;

    private final IAuthzOutboxEventRepository repository;
    private final AuthzMqProperties properties;
    private final ObjectProvider<MessageSender> senderProvider;
    private final ObjectProvider<ResourceReadModelProjector> readModelProjectorProvider;
    private final ObjectMapper objectMapper;

    private final ReentrantLock lock = new ReentrantLock();
    private final AtomicLong lastUnavailableWarnAt = new AtomicLong(0L);

    /**
     * 立即投递的专用执行器：单线程、守护线程，避免阻塞业务线程（见类注释）。
     */
    private final ExecutorService wakeupExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "authz-outbox-immediate");
        thread.setDaemon(true);
        return thread;
    });

    public AuthzOutboxPublisher(IAuthzOutboxEventRepository repository,
                                AuthzMqProperties properties,
                                ObjectProvider<MessageSender> senderProvider,
                                ObjectProvider<ResourceReadModelProjector> readModelProjectorProvider,
                                ObjectMapper objectMapper) {
        this.repository = repository;
        this.properties = properties;
        this.senderProvider = senderProvider;
        this.readModelProjectorProvider = readModelProjectorProvider;
        this.objectMapper = objectMapper;
    }

    /**
     * 兜底轮询：定时捞出到期事件并投递。
     */
    @Scheduled(fixedDelayString = "${app.authz.mq.outbox.poll-interval-ms:1000}")
    public void poll() {
        if (!active()) {
            return;
        }
        try {
            publishDue();
        } catch (Exception e) {
            // 轮询任务绝不能因为单次异常而停摆；显式记录后下一轮继续。
            log.error("Outbox 轮询发生异常，下一轮继续", e);
        }
    }

    /**
     * 事务提交后的立即投递（低延迟通道）。投递在独立线程上执行，不阻塞调用方。
     *
     * @param outboxId Outbox 行 ID；为空或已非 PENDING 时为 no-op
     */
    public void publishNow(Long outboxId) {
        if (outboxId == null || !active()) {
            return;
        }
        try {
            wakeupExecutor.execute(() -> doPublishNow(outboxId));
        } catch (RejectedExecutionException e) {
            // 执行器已关闭（应用停机中）：该行仍是 PENDING，停机前的最后一次轮询或下次启动后的轮询会兜住。
            log.debug("Outbox 立即投递被拒绝（执行器已关闭），交由轮询兜底: id={}", outboxId);
        }
    }

    private void doPublishNow(Long outboxId) {
        MessageSender sender = senderProvider.getIfAvailable();
        if (sender == null) {
            warnSenderUnavailable();
            return;
        }
        lock.lock();
        try {
            repository.findById(outboxId)
                    .filter(event -> event.getStatus() == AuthzOutboxStatus.PENDING)
                    .ifPresent(event -> deliver(event, sender));
        } catch (Exception e) {
            log.warn("Outbox 立即投递失败（将由轮询兜底重试）: id={}", outboxId, e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 停机时关闭立即投递执行器。未完成的事件仍在库中为 {@code PENDING}，下次启动由轮询投递。
     */
    @Override
    public void destroy() {
        wakeupExecutor.shutdown();
        log.info("Outbox 立即投递执行器已关闭");
    }

    private void publishDue() {
        if (senderProvider.getIfAvailable() == null) {
            warnSenderUnavailable();
            return;
        }
        PageRequest limit = PageRequest.of(0, Math.max(1, properties.getOutbox().getBatchSize()));
        List<AuthzOutboxEvent> due = repository.findDue(AuthzOutboxStatus.PENDING, LocalDateTime.now(), limit);
        if (due.isEmpty()) {
            return;
        }
        lock.lock();
        try {
            MessageSender sender = senderProvider.getIfAvailable();
            if (sender == null) {
                return;
            }
            for (AuthzOutboxEvent event : due) {
                deliver(event, sender);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 单条投递。调用方必须持有 {@link #lock}。
     */
    private void deliver(AuthzOutboxEvent event, MessageSender sender) {
        try {
            // 3.8 第 1 点：先刷 Redis 读模型，再发 MQ。刷新失败 => 抛异常 => 进入退避重试。
            refreshReadModel(event);

            MqMessage<Object> message = MqMessage.<Object>builder()
                    .topic(event.getEventType())
                    .payload(deserialize(event))
                    .bizKey(event.getBizKey())
                    .keys(event.getBizKey())
                    .build();

            CommonSendResult result = sender.send(message);
            if (isAcknowledged(result)) {
                markSent(event);
            } else {
                markFailed(event, "Broker 未确认: " + (result == null ? "null" : result.getBrokerStatus()));
            }
        } catch (Exception e) {
            markFailed(event, describe(e));
        }
    }

    /**
     * 投递前刷新读模型。{@code RESOURCE_CHANGED} → {@code auth:resource:{serviceId}}。
     *
     * <p>{@code PERMISSION_CHANGED} 的 {@code auth:authorization:*} 投影为独立跟进项（8.12），
     * 此处不做处理——网关失效后回源会退化为 Dubbo，功能正确但少一层缓存收益。</p>
     */
    private void refreshReadModel(AuthzOutboxEvent event) {
        if (!properties.isRefreshReadModelBeforePublish()
                || !properties.getResourceChangedTopic().equals(event.getEventType())
                || event.getRefKey() == null || event.getRefKey().isBlank()) {
            return;
        }
        ResourceReadModelProjector projector = readModelProjectorProvider.getIfAvailable();
        if (projector != null) {
            projector.project(event.getRefKey());
        }
    }

    private Object deserialize(AuthzOutboxEvent event) {
        String type = event.getEventType();
        if (properties.getResourceChangedTopic().equals(type)) {
            return objectMapper.readValue(event.getPayload(), ResourceChangedEvent.class);
        }
        if (properties.getPermissionChangedTopic().equals(type)) {
            return objectMapper.readValue(event.getPayload(), PermissionChangedEvent.class);
        }
        // 未知类型：原样以字符串投递，避免整条链路因为一个未来事件类型而卡住。
        log.warn("Outbox 遇到未知事件类型，按原始文本投递: id={}, eventType={}", event.getId(), type);
        return event.getPayload();
    }

    private void markSent(AuthzOutboxEvent event) {
        event.setStatus(AuthzOutboxStatus.SENT);
        event.setSentAt(LocalDateTime.now());
        event.setLastError(null);
        repository.save(event);
        log.info("Outbox 事件已投递: id={}, eventType={}, bizKey={}, attempts={}",
                event.getId(), event.getEventType(), event.getBizKey(), event.getAttempts());
    }

    private void markFailed(AuthzOutboxEvent event, String error) {
        int attempts = event.getAttempts() + 1;
        event.setAttempts(attempts);
        event.setLastError(truncate(error));
        if (attempts >= Math.max(1, properties.getOutbox().getMaxAttempts())) {
            event.setStatus(AuthzOutboxStatus.FAILED);
            repository.save(event);
            log.error("Outbox 事件投递达到最大尝试次数，置为 FAILED 待人工重放: id={}, eventType={}, bizKey={}, attempts={}, error={}",
                    event.getId(), event.getEventType(), event.getBizKey(), attempts, error);
            return;
        }
        event.setNextRetryAt(LocalDateTime.now().plusNanos(backoffMillis(attempts) * 1_000_000L));
        repository.save(event);
        log.warn("Outbox 事件投递失败，将退避重试: id={}, eventType={}, attempts={}, nextRetryAt={}, error={}",
                event.getId(), event.getEventType(), attempts, event.getNextRetryAt(), error);
    }

    /**
     * 指数退避：{@code base * 2^(attempts-1)}，cap 到 {@code maxBackoffMs}。
     */
    private long backoffMillis(int attempts) {
        long base = Math.max(1L, properties.getOutbox().getInitialBackoffMs());
        long max = Math.max(base, properties.getOutbox().getMaxBackoffMs());
        int shift = Math.min(attempts - 1, 20);
        long value = base << shift;
        return value <= 0 ? max : Math.min(value, max);
    }

    /**
     * 判断 Broker 是否已确认接收。
     *
     * <p>{@code SEND_OK} 为成功；{@code REJECTED} 表示被拦截器<b>主动</b>阻断，视为已终结
     * （不应重试）；其余（如 {@code FLUSH_DISK_TIMEOUT}）视为未确认，进入重试。</p>
     */
    private boolean isAcknowledged(CommonSendResult result) {
        if (result == null || result.getBrokerStatus() == null) {
            return false;
        }
        String status = result.getBrokerStatus();
        return status.startsWith(SEND_OK_PREFIX) || REJECTED.equals(status);
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
}
