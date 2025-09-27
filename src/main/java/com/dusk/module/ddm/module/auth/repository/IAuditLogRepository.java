package com.dusk.module.ddm.module.auth.repository;

import com.dusk.common.core.repository.IBaseRepository;
import com.dusk.module.auth.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;

/**
 * @author kefuming
 * @date 2020-04-29 15:47
 */
public interface IAuditLogRepository extends IBaseRepository<AuditLog> {
    @EntityGraph(AuditLog.NamedEntityGraph_createUser)
    @Override
    Page<AuditLog> findAll(Specification<AuditLog> spec, Pageable pageable);
}
