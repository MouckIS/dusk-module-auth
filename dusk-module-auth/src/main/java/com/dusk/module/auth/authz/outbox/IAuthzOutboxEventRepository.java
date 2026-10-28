package com.dusk.module.auth.authz.outbox;

import com.dusk.common.core.repository.IBaseRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Outbox 事件仓储，见《权限优化方案-整理版》8.5。
 */
public interface IAuthzOutboxEventRepository extends IBaseRepository<AuthzOutboxEvent> {

    /**
     * 取一批「到期待投」的事件。
     *
     * <p>按 {@code id} 升序，保证同一业务键的事件按产生顺序投递（Outbox 写在与业务同事务内，
     * id 由雪花算法生成、时间有序）。{@code nextRetryAt} 为空表示从未失败，立即可投。</p>
     */
    @Query("select e from AuthzOutboxEvent e "
            + "where e.status = :status and (e.nextRetryAt is null or e.nextRetryAt <= :now) "
            + "order by e.id asc")
    List<AuthzOutboxEvent> findDue(@Param("status") AuthzOutboxStatus status,
                                   @Param("now") LocalDateTime now,
                                   Pageable pageable);

    /**
     * 统计某状态的事件数，用于运维与告警。
     */
    long countByStatus(AuthzOutboxStatus status);
}
