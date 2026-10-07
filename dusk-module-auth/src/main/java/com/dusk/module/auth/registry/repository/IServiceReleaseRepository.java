package com.dusk.module.auth.registry.repository;

import com.dusk.common.core.repository.IBaseRepository;
import com.dusk.module.auth.registry.entity.ServiceRelease;
import com.dusk.module.auth.registry.enums.ReleaseStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 服务发布版本仓库，见《权限优化方案-整理版》4.5 / 4.7。
 */
public interface IServiceReleaseRepository extends IBaseRepository<ServiceRelease> {

    /**
     * 按服务标识 + 发布版本定位 Release，注册与 Diff 的入口查询。
     */
    Optional<ServiceRelease> findByService_ServiceIdAndServiceVersion(String serviceId, String serviceVersion);

    /**
     * 某服务下的全部 Release。
     */
    List<ServiceRelease> findByService_ServiceId(String serviceId);

    /**
     * 按状态集合查询 Release，用于 Retired 同步与 Permission 生效性判定。
     */
    List<ServiceRelease> findByStatusIn(Collection<ReleaseStatus> statuses);
}
