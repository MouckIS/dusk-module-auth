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
     * 按服务标识 + 发布版本定位 Release。保留用于兼容旧数据与迁移期查询。
     */
    Optional<ServiceRelease> findByService_ServiceIdAndServiceVersion(String serviceId, String serviceVersion);

    /**
     * 按不可变身份 {@code (serviceId, serviceVersion, resourceVersion)} 定位 Release，见 4.2。
     */
    Optional<ServiceRelease> findByService_ServiceIdAndServiceVersionAndResourceVersion(
            String serviceId, String serviceVersion, String resourceVersion);

    /**
     * 某服务下指定状态的 Release。
     */
    List<ServiceRelease> findByService_ServiceIdAndStatusIn(String serviceId, Collection<ReleaseStatus> statuses);

    /**
     * 某服务下的全部 Release。
     */
    List<ServiceRelease> findByService_ServiceId(String serviceId);

    /**
     * 按状态集合查询 Release，用于 Retired 同步与 Permission 生效性判定。
     */
    List<ServiceRelease> findByStatusIn(Collection<ReleaseStatus> statuses);
}
