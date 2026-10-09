package com.dusk.module.auth.registry.enums;

import com.dusk.common.core.exception.BusinessException;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
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

    /**
     * 可能承接流量的状态集合，见 {@link #mayServeTraffic()}。
     */
    private static final List<ReleaseStatus> TRAFFIC_CAPABLE = List.of(REGISTERED, ACTIVE);

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
     *
     * <p><b>注意</b>：本方法与 {@link #mayServeTraffic()} 的口径不同，差别见后者的说明；
     * 注册链路（资源落库、运行读模型投影、权限生效性判定）统一使用
     * {@link #mayServeTraffic()}，本方法保留以示 4.8 的原始表述。</p>
     */
    public boolean keepsPermissionsAlive() {
        return this == ACTIVE;
    }

    /**
     * 该 Release 是否<b>可能承接流量</b>，因而其 Resource 必须进入运行映射（5.7）并参与权限生效性判定（4.8）。
     *
     * <p>取 {@link #REGISTERED} 与 {@link #ACTIVE}、排除 {@link #RETIRED}，理由是 4.6/4.7 的既定前提
     * <b>尚未落地</b>：现状各服务未向 Nacos 发布 {@code version} metadata，Auth 也没有实例同步任务，
     * 因此 {@code Instance → Release} 的归属无从得知，Release 无法按 4.7 由「存在运行实例」推进到 ACTIVE。</p>
     *
     * <p>而 3.12 明确 readiness 以「Auth 注册确认」为准、不以缓存收敛为准——即<b>注册成功就意味着该版本即将接流量</b>。
     * 若此处按 {@code ACTIVE} 单值过滤，新服务注册后其全部路由都不会进入运行读模型，
     * 网关在 5.10 的「默认拒绝」下会把该服务的所有接口判为「未登记」而 403，方案 3.5 的「窗口归零」直接失效。
     * 因此注册链路必须把 {@code REGISTERED} 一并视作「可能承接流量」。</p>
     *
     * <p>待 4.6 的 Nacos metadata 与实例同步落地后，可把本方法收紧为 {@code this == ACTIVE}——
     * 注册链路的所有判定都集中在这一处，收紧不会有遗漏。</p>
     */
    public boolean mayServeTraffic() {
        return this == REGISTERED || this == ACTIVE;
    }

    /**
     * {@link #mayServeTraffic()} 对应的状态集合，供 SQL 查询与批量过滤直接使用（只读）。
     */
    public static List<ReleaseStatus> trafficCapableStatuses() {
        return TRAFFIC_CAPABLE;
    }
}
