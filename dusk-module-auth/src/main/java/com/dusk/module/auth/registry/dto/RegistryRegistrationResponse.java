package com.dusk.module.auth.registry.dto;

import java.util.List;

/**
 * 一次快照注册的结果，返回给 SDK（见《权限优化方案-整理版》3.4「注册确认」）。
 *
 * <p>SDK 侧只在 2xx 时判断「已接纳」，正文对它没有强约束（{@code SyncRegisterClient}
 * 仅把非 2xx 的正文片段带进日志）。本结构存在的目的是让<b>运维与排障</b>能直接看到
 * 这次注册改动了什么：新接口、被废弃接口、换绑待办、权限进入/离开 ORPHANED。</p>
 *
 * <p>各资源清单来自本次实际施加的变更（不是 Diff 报告）：首次注册一个新 Release 时，
 * 与上一版本相同的接口不会出现在 Diff 里，但仍必须在本 Release 下落库，它们会出现在
 * {@link #addedResources} 中。</p>
 *
 * @param serviceId              服务标识
 * @param serviceVersion         发布版本
 * @param resourceVersion        Auth 已接纳的 API 定义版本
 * @param unchanged              {@code true} 表示本次上报的版本与该 Release 已接纳版本一致，未做任何写库
 * @param releaseStatus          该 Release 当前状态
 * @param totalResources         快照中的资源总数（受保护 + 匿名）
 * @param addedResources         本次新建的资源
 * @param updatedResources       本次原地更新的资源（元数据变化 / 回滚恢复）
 * @param deprecatedResources    本次标记为废弃的资源
 * @param pendingRebindResources 本次挂起等待管理员确认的换绑
 * @param activatedPermissions   本次转为生效中（含新建）的权限码
 * @param orphanedPermissions    本次转入已孤立的权限码（无任何有效 API 使用，授权关系保留）
 */
public record RegistryRegistrationResponse(
        String serviceId,
        String serviceVersion,
        String resourceVersion,
        boolean unchanged,
        String releaseStatus,
        int totalResources,
        List<String> addedResources,
        List<String> updatedResources,
        List<String> deprecatedResources,
        List<String> pendingRebindResources,
        List<String> activatedPermissions,
        List<String> orphanedPermissions) {

    public RegistryRegistrationResponse {
        addedResources = List.copyOf(addedResources == null ? List.of() : addedResources);
        updatedResources = List.copyOf(updatedResources == null ? List.of() : updatedResources);
        deprecatedResources = List.copyOf(deprecatedResources == null ? List.of() : deprecatedResources);
        pendingRebindResources = List.copyOf(pendingRebindResources == null ? List.of() : pendingRebindResources);
        activatedPermissions = List.copyOf(activatedPermissions == null ? List.of() : activatedPermissions);
        orphanedPermissions = List.copyOf(orphanedPermissions == null ? List.of() : orphanedPermissions);
    }

    /**
     * 幂等命中：版本未变、未写库。
     */
    public static RegistryRegistrationResponse unchanged(String serviceId, String serviceVersion,
                                                        String resourceVersion, String releaseStatus,
                                                        int totalResources) {
        return new RegistryRegistrationResponse(serviceId, serviceVersion, resourceVersion, true, releaseStatus,
                totalResources, List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }
}
