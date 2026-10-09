package com.dusk.module.auth.registry.repository;

import com.dusk.common.core.repository.IBaseRepository;
import com.dusk.module.auth.registry.entity.ApiResource;
import com.dusk.module.auth.registry.enums.ReleaseStatus;
import com.dusk.module.auth.registry.enums.ResourceStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * 查询引用某权限码的全部资源，用于判定权限是否仍被「可能承接流量的 Release」使用（4.8）。
     *
     * <p><b>为什么同时匹配 {@code pendingPermissionCode}</b>：换绑（4.9）的候选权限虽然尚未生效，
     * 但它已由某个 API 声明，属于「被 API 使用」，不应被判定为 ORPHANED；
     * 管理员在换绑待办中也需要它作为可见权限点。方向上是保守的一侧：
     * 只会让权限更晚进入 ORPHANED，不会让仍在生效的权限被回收。</p>
     *
     * <p>本方法只按 Release 状态过滤；资源自身的 {@code DEPRECATED} 由调用方用
     * {@code ResourceStatus.effectiveForRuntime()} 过滤——两个维度的语义不同，不合并进 SQL。</p>
     */
    @Query("select r from ApiResource r join r.release rel "
            + "where (r.permissionCode = :code or r.pendingPermissionCode = :code) "
            + "and rel.status in :releaseStatuses")
    List<ApiResource> findPermissionReferences(@Param("code") String code,
                                               @Param("releaseStatuses") Collection<ReleaseStatus> releaseStatuses);
}
