package com.dusk.module.auth.registry.lifecycle;

import java.util.List;

/**
 * 某个 Service 的实例分布观测输入，见《权限优化方案-整理版》4.7。
 *
 * <p>{@link #defaultReleaseVersion} 对应「被部署系统标记为当前有效版本」（4.7）：
 * 该版本即使暂时 0 实例也应保持 ACTIVE，避免「刚发布就退役」。</p>
 *
 * @param serviceId             服务标识
 * @param releases              该服务下已知的全部 Release 观测
 * @param defaultReleaseVersion 部署系统标记的当前默认版本，可为 {@code null}
 */
public record ServiceInstanceState(
        String serviceId,
        List<ReleaseInstanceState> releases,
        String defaultReleaseVersion) {

    public ServiceInstanceState {
        if (serviceId == null || serviceId.isBlank()) {
            throw new IllegalArgumentException("ServiceInstanceState.serviceId 不能为空");
        }
        serviceId = serviceId.trim();
        releases = releases == null ? List.of() : List.copyOf(releases);
        defaultReleaseVersion = defaultReleaseVersion == null ? null : defaultReleaseVersion.trim();
    }

    /**
     * 全部 Release 的实例总数。
     */
    public int totalInstances() {
        return releases.stream().mapToInt(ReleaseInstanceState::instanceCount).sum();
    }

    /**
     * 整个 Service 是否 0 实例，即 4.7 第二级防抖「环境停机保护」的触发条件。
     */
    public boolean fullyDown() {
        return totalInstances() == 0;
    }
}
