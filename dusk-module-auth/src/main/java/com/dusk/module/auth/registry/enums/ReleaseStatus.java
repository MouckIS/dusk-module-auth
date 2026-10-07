package com.dusk.module.auth.registry.enums;

import com.dusk.common.core.exception.BusinessException;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 服务发布版本（Release）生命周期状态，见《权限优化方案-整理版》4.5 / 4.7。
 *
 * <p>Release 必须独立于 Service 存在，否则无法解决滚动发布期间
 * 「instance-1 还在 1.0.0、instance-2 已是 1.1.0」的多版本并存问题。
 * 只要旧 Release 仍为 {@link #ACTIVE}，其独有 Resource 对应的 Permission 就必须保持生效。</p>
 */
public enum ReleaseStatus {

    /**
     * 已注册：SDK 完成注册，但尚无运行实例，也未被部署系统标记为当前有效版本。
     */
    REGISTERED("已注册", "SDK 已注册，尚无运行实例且未被标记为当前有效版本"),

    /**
     * 生效中：存在运行实例，或被部署系统标记为当前有效版本。
     */
    ACTIVE("生效中", "存在运行实例，或被部署系统标记为当前有效版本"),

    /**
     * 已退役：持续 5 分钟无运行实例（4.7 防抖通过）。其独有 Permission 可进入 ORPHANED。
     */
    RETIRED("已退役", "持续无运行实例，其独有 Resource 不再支撑任何 Permission");

    /**
     * 合法迁移表。
     *
     * <p>{@code REGISTERED → RETIRED} 是对 4.7 状态图的<b>必要扩展</b>：
     * 一个 Release 可能在拿到实例之前就被回滚（注册完成后立刻撤下），
     * 若不允许该迁移，它会永久停留在 REGISTERED，导致其 Resource 永远无法退役。</p>
     */
    private static final Map<ReleaseStatus, Set<ReleaseStatus>> ALLOWED_TRANSITIONS = Map.of(
            REGISTERED, EnumSet.of(ACTIVE, RETIRED),
            ACTIVE, EnumSet.of(RETIRED),
            RETIRED, EnumSet.of(ACTIVE)
    );

    private final String displayName;
    private final String description;

    ReleaseStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public Set<ReleaseStatus> allowedTargets() {
        return Collections.unmodifiableSet(ALLOWED_TRANSITIONS.get(this));
    }

    public boolean canTransitionTo(ReleaseStatus target) {
        if (target == null) {
            return false;
        }
        if (target == this) {
            return true;
        }
        return ALLOWED_TRANSITIONS.get(this).contains(target);
    }

    /**
     * 执行迁移；非法迁移直接失败（Fail Fast）。
     *
     * @throws BusinessException 当迁移不合法时
     */
    public ReleaseStatus transitionTo(ReleaseStatus target) {
        if (target == null) {
            throw new BusinessException("Release 状态迁移目标不能为空，当前状态：" + this);
        }
        if (!canTransitionTo(target)) {
            throw new BusinessException(
                    "非法的 Release 状态迁移：" + this + " -> " + target + "，允许的后继状态：" + allowedTargets());
        }
        return target;
    }

    /**
     * 该 Release 是否正在支撑其 Resource 的生效性。
     *
     * <p>只有 {@link #ACTIVE} 会阻止其独有 Permission 被判定为 ORPHANED。</p>
     */
    public boolean keepsPermissionsAlive() {
        return this == ACTIVE;
    }
}
