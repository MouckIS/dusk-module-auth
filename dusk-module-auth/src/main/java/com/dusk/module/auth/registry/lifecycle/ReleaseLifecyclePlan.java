package com.dusk.module.auth.registry.lifecycle;

import com.dusk.module.auth.registry.enums.ReleaseStatus;

import java.util.List;

/**
 * 某个 Service 在本轮同步中的 Release 状态迁移计划，见《权限优化方案-整理版》4.7。
 *
 * @param serviceId    服务标识
 * @param frozen       是否因「环境停机保护」被冻结：为 {@code true} 时不允许任何状态迁移
 * @param transitions  需要应用的迁移决定（冻结时为空列表）
 * @param freezeReason 冻结原因，用于审计；未冻结时为 {@code null}
 */
public record ReleaseLifecyclePlan(
        String serviceId,
        boolean frozen,
        List<ReleaseTransition> transitions,
        String freezeReason) {

    public ReleaseLifecyclePlan {
        transitions = transitions == null ? List.of() : List.copyOf(transitions);
    }

    /**
     * 本轮是否存在需要落库的变化。
     */
    public boolean hasChanges() {
        return !transitions.isEmpty();
    }

    /**
     * 本轮是否有 Release 进入 RETIRED。此结果会触发下游 Permission 的生效性重算（4.8）。
     */
    public boolean hasRetirement() {
        return transitions.stream().anyMatch(transition -> transition.statusChanged()
                && transition.to() == ReleaseStatus.RETIRED);
    }
}
