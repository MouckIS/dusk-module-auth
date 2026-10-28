package com.dusk.module.auth.registry.enums;

import com.dusk.common.core.exception.BusinessException;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * API 资源（Resource）生命周期状态，见《权限优化方案-整理版》4.1 / 4.9 / 4.11。
 *
 * <p>资源与权限必须分离：API 被删除时资源进入 {@link #DEPRECATED}，
 * 但对应的 Permission 与 RolePermission 必须保留（红线②「API 删除 ≠ Permission 删除」）。</p>
 *
 * <p>换绑（同 {@code (method, path)} 但 permission 变化）不自动生效：
 * 资源先进入 {@link #PENDING_REBIND}，运行时仍按旧绑定执行，
 * 直到管理员确认（→ {@link #ACTIVE}）或拒绝（→ {@link #REBIND_REJECTED}）。</p>
 */
public enum ResourceStatus {

    /**
     * 正常：资源存在且绑定已被接纳，参与运行时映射。
     */
    ACTIVE("正常", "资源绑定已接纳，参与运行时授权映射"),

    /**
     * 待确认换绑：快照上报了不同的 permission，等管理员确认；运行时仍按旧绑定执行。
     */
    PENDING_REBIND("待确认换绑", "同一路径的权限绑定发生变化，运行时仍按旧绑定执行，等待管理员确认"),

    /**
     * 换绑已拒绝：管理员拒绝本次换绑，资源保持旧绑定；
     * 若下次快照仍上报相同的新权限，则重新回到 {@link #PENDING_REBIND} 再次告警。
     */
    REBIND_REJECTED("换绑已拒绝", "管理员拒绝换绑，资源保持旧绑定；重复上报时重新告警"),

    /**
     * 已废弃：资源已从所属 Release 的快照中消失。
     */
    DEPRECATED("已废弃", "资源已从 Release 快照中消失，等待所有旧 Release 退役");

    /**
     * 合法迁移表。
     *
     * <p>两侧说明：</p>
     * <ul>
     *   <li>{@code ACTIVE → DEPRECATED}：资源从快照中消失（红线② 要求 Permission 侧不受影响）。</li>
     *   <li>{@code PENDING_REBIND → ACTIVE}：管理员确认换绑，或开发者回滚了绑定改动。</li>
     *   <li>{@code REBIND_REJECTED → ACTIVE}：开发者回滚，快照重新与被接纳的绑定一致，告警自然消除。</li>
     *   <li>{@code DEPRECATED → ACTIVE}：旧 Release 回滚重新上线（4.11），无需重建 Resource。</li>
     *   <li>{@code DEPRECATED → PENDING_REBIND}：资源随回滚重新出现，但绑定的 permission 与历史值不同。</li>
     * </ul>
     */
    private static final Map<ResourceStatus, Set<ResourceStatus>> ALLOWED_TRANSITIONS = Map.of(
            ACTIVE, EnumSet.of(PENDING_REBIND, DEPRECATED),
            PENDING_REBIND, EnumSet.of(ACTIVE, REBIND_REJECTED, DEPRECATED),
            REBIND_REJECTED, EnumSet.of(ACTIVE, PENDING_REBIND, DEPRECATED),
            DEPRECATED, EnumSet.of(ACTIVE, PENDING_REBIND)
    );

    private final String displayName;
    private final String description;

    ResourceStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 合法后继状态集合（只读）。
     */
    public Set<ResourceStatus> allowedTargets() {
        return Collections.unmodifiableSet(ALLOWED_TRANSITIONS.get(this));
    }

    /**
     * 判断能否迁移到目标状态。自身到自身视为幂等的空操作，返回 {@code true}。
     */
    public boolean canTransitionTo(ResourceStatus target) {
        if (target == null) {
            return false;
        }
        if (target == this) {
            return true;
        }
        return ALLOWED_TRANSITIONS.get(this).contains(target);
    }

    /**
     * 执行迁移；非法迁移直接失败（Fail Fast），避免状态被静默改坏。
     *
     * @throws BusinessException 当迁移不合法时
     */
    public ResourceStatus transitionTo(ResourceStatus target) {
        if (target == null) {
            throw new BusinessException("Resource 状态迁移目标不能为空，当前状态：" + this);
        }
        if (!canTransitionTo(target)) {
            throw new BusinessException(
                    "非法的 Resource 状态迁移：" + this + " -> " + target + "，允许的后继状态：" + allowedTargets());
        }
        return target;
    }

    /**
     * 该状态下的资源是否参与运行时授权映射。
     *
     * <p>换绑未确认期间必须继续按旧绑定放行（4.9「不自动生效」），
     * 因此 {@link #PENDING_REBIND} 与 {@link #REBIND_REJECTED} 都算生效中。</p>
     */
    public boolean effectiveForRuntime() {
        return this == ACTIVE || this == PENDING_REBIND || this == REBIND_REJECTED;
    }
}
