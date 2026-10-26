package com.dusk.module.auth.registry.repository;

import com.dusk.common.core.repository.IBaseRepository;
import com.dusk.module.auth.registry.entity.ServiceDefinition;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 服务定义仓库，见《权限优化方案-整理版》2.2。
 */
public interface IServiceDefinitionRepository extends IBaseRepository<ServiceDefinition> {

    /**
     * 按服务标识查找。{@code (serviceId, serviceVersion)} 是注册幂等的第一层定位。
     */
    Optional<ServiceDefinition> findByServiceId(String serviceId);

    /**
     * 注册事务内锁定服务行，串行化同一服务的 {@code resourceRevision} 推进（3.2）。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ServiceDefinition s where s.serviceId = :serviceId")
    Optional<ServiceDefinition> findByServiceIdForUpdate(@Param("serviceId") String serviceId);
}
