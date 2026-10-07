package com.dusk.module.auth.registry.enums;

import com.dusk.common.core.exception.BusinessException;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 业务权限点（Permission）生命周期状态，见《权限优化方案-整理版》4.3 / 4.8 / 4.11。
 *
 * <p>核心约束：<b>代码发布不能修改管理员的授权决策</b>。因此 Resource 与 Permission 的生命周期解耦，
 * 并且状态迁移必须携带 {@link TransitionTrigger}：</p>
 * <ul>
 *   <li>{@code ACTIVE → ORPHANED} 仅 {@link TransitionTrigger#AUTO}（4.8）；</li>
 *   <li>{@code ORPHANED → ACTIVE} 仅 {@link TransitionTrigger#AUTO}（回滚自动恢复，4.11）；</li>
 *   <li>{@code ORPHANED → DISABLED} 仅 {@link TransitionTrigger#MANUAL}（管理员手工禁用）；</li>
 *   <li>{@code DISABLED → ACTIVE} 仅 {@link TransitionTrigger#MANUAL}，
 *       即管理员在抖动期的禁用操作不会被代码部署静默回滚。</li>
 * </ul>
 */
public enum PermissionStatus {

    /**
     * 生效中：至少被一个 ACTIVE Release 使用。
     */
    ACTIVE("生效中", "至少被一个生效中的 Release 使用"),

    /**
     * 已孤立：没有任何 ACTIVE Release 使用，等待管理员处置；RolePermission 始终保留。
     */
    ORPHANED("已孤立", "当前没有任何有效 API 使用该权限，等待管理员处置（授权关系保留）"),

    /**
     * 已禁用：管理员确认后手工禁用。
     */
    DISABLED("已禁用", "管理员手工禁用，不会被部署动作自动恢复");

    /**
     * 合法迁移表：{@code 源状态 -> 目标状态 -> 允许的触发来源}。
     */
    private static final Map<PermissionStatus, Map<PermissionStatus, Set<TransitionTrigger>>> ALLOWED_TRANSITIONS =
            buildTransitions();

    private static Map<PermissionStatus, Map<PermissionStatus, Set<TransitionTrigger>>> buildTransitions() {
        return Map.of(
                ACTIVE, Map.of(
                        ORPHANED, EnumSet.of(TransitionTrigger.AUTO)
                ),
                ORPHANED, Map.of(
                        ACTIVE, EnumSet.of(TransitionTrigger.AUTO),
                        DISABLED, EnumSet.of(TransitionTrigger.MANUAL)
                ),
                DISABLED, Map.of(
                        ACTIVE, EnumSet.of(TransitionTrigger.MANUAL)
                )
        );
    }

    private final String displayName;
    private final String description;

    PermissionStatus(String displayName, String description) {
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
     * 在给定触发来源下，可迁移到的目标状态集合（只读）。
     */
    public Set<PermissionStatus> allowedTargets(TransitionTrigger trigger) {
        if (trigger == null) {
            return Collections.emptySet();
        }
        EnumSet<PermissionStatus> targets = EnumSet.noneOf(PermissionStatus.class);
        ALLOWED_TRANSITIONS.get(this)
                .forEach((target, triggers) -> {
                    if (triggers.contains(trigger)) {
                        targets.add(target);
                    }
                });
        return Collections.unmodifiableSet(targets);
    }

    /**
     * 判断在给定触发来源下能否迁移到目标状态。自身到自身视为幂等的空操作，返回 {@code true}。
     */
    public boolean canTransitionTo(PermissionStatus target, TransitionTrigger trigger) {
        if (target == null || trigger == null) {
            return false;
        }
        if (target == this) {
            return true;
        }
        return ALLOWED_TRANSITIONS.get(this)
                .getOrDefault(target, Collections.emptySet())
                .contains(trigger);
    }

    /**
     * 执行迁移；非法迁移直接失败（Fail Fast）。
     *
     * @throws BusinessException 当迁移不合法时
     */
    public PermissionStatus transitionTo(PermissionStatus target, TransitionTrigger trigger) {
        if (target == null) {
            throw new BusinessException("Permission 状态迁移目标不能为空，当前状态：" + this);
        }
        if (trigger == null) {
            throw new BusinessException("Permission 状态迁移触发来源不能为空，当前状态：" + this);
        }
        if (!canTransitionTo(target, trigger)) {
            throw new BusinessException("非法的 Permission 状态迁移：" + this + " -> " + target
                    + "（触发来源 " + trigger + "），允许的后继状态：" + allowedTargets(trigger));
        }
        return target;
    }

    /**
     * 该权限是否参与运行时授权判定。
     *
     * <p>ORPHANED 表示「没有 API 再用它」，但授权关系仍然保留（4.3 note），
     * 因此与 ACTIVE 一样参与判定；只有管理员手工 DISABLED 才真正阻断。</p>
     */
    public boolean effectiveForRuntime() {
        return this == ACTIVE || this == ORPHANED;
    }
}
