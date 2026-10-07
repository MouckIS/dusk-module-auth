package com.dusk.module.auth.registry.repository;

import com.dusk.common.core.repository.IBaseRepository;
import com.dusk.module.auth.registry.entity.ApiResource;
import com.dusk.module.auth.registry.enums.ReleaseStatus;
import com.dusk.module.auth.registry.enums.ResourceStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * API 资源仓库，见《权限优化方案-整理版》4.1 / 4.8。
 */
public interface IApiResourceRepository extends IBaseRepository<ApiResource> {

    /**
     * 某 Release 下的全部资源，Diff 的「旧集合」来源。
     */
    List<ApiResource> findByRelease_Id(Long releaseId);

    /**
     * 指定 Release 范围内、给定状态的资源。
     */
    List<ApiResource> findByRelease_IdInAndStatusIn(Collection<Long> releaseIds, Collection<ResourceStatus> statuses);

    /**
     * 精确定位单个资源（红线⑫：路径精确唯一，禁止依赖 Ant 模式遍历顺序）。
     */
    Optional<ApiResource> findByRelease_IdAndHttpMethodAndPath(Long releaseId, String httpMethod, String path);

    /**
     * 判定某个 Permission 是否仍被有效引用（4.8）。
     *
     * <p>调用方还需过滤掉资源自身的 {@code DEPRECATED} 状态，
     * 因为「Release 仍 ACTIVE」只说明发布版本在线，不代表该资源还在快照里。</p>
     */
    List<ApiResource> findByPermissionCodeAndRelease_Status(String permissionCode, ReleaseStatus releaseStatus);
}
