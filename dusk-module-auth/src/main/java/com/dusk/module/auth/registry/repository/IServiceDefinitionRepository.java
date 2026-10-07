package com.dusk.module.auth.registry.repository;

import com.dusk.common.core.repository.IBaseRepository;
import com.dusk.module.auth.registry.entity.ServiceDefinition;

import java.util.Optional;

/**
 * 服务定义仓库，见《权限优化方案-整理版》2.2。
 */
public interface IServiceDefinitionRepository extends IBaseRepository<ServiceDefinition> {

    /**
     * 按服务标识查找。{@code (serviceId, serviceVersion)} 是注册幂等的第一层定位。
     */
    Optional<ServiceDefinition> findByServiceId(String serviceId);
}
