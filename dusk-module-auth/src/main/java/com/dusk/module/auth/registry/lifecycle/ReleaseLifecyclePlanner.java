package com.dusk.module.auth.registry.lifecycle;

import com.dusk.common.core.exception.BusinessException;
import com.dusk.module.auth.registry.enums.ReleaseStatus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Release 退役判定，见《权限优化方案-整理版》4.7。
 *
 * <p>Auth 不自己猜 Release 是否退役，而是由「Nacos 实例状态 + 定时同步」共同决定，
 * 并叠加两级防抖：</p>
 * <ol>
 *   <li><b>单 Release 级</b>：0 实例持续 {@value #DEFAULT_RETIREMENT_GRACE_MINUTES} 分钟才进入 RETIRED，
 *       避免服务重启导致 Permission 瞬间消失；</li>
 *   <li><b>Service 级环境停机保护</b>：若该 Service 的<b>全部</b> Release 均 0 实例
 *       （典型场景是整个环境停机维护），则冻结该 Service 的<b>全部</b>状态迁移。
 *       否则停机 5 分钟以上会触发「批量 RETIRED → Permission 批量 ORPHANED → 实例回来再批量 ACTIVE」的抖动。</li>
 * </ol>
 *
 * <p><b>冻结语义（本实现的取择）</b>：冻结期间既不迁移状态，也<b>不重置</b>各 Release 的
 * {@code zeroInstanceSince} 计时——「暂停一切状态迁移」按字面理解为挂起而非清零。
 * 环境恢复后，真正没有实例的 Release 会按其原始计时立即退役，不会因为停机而无限期滞留。</p>
 *
 * <p><b>已知边界（建议后续做产品决策）</b>：按 4.7 的字面规则，只要「全部 Release 均 0 实例」即冻结。
 * 因此一个<b>只有单个 Release</b> 的服务一旦实例全部下线，也会被判定为环境停机保护而冻结，
 * 其 Release 永不自动退役。对「服务被永久下线 / 注销」的场景，需要有显式的管理端退役操作兜底，
 * 或引入「曾经有过实例」的历史标记来区分「环境停机」与「服务注销」。
 * 本实现遵循文档字面规则，不擅自引入该状态。</p>
 *
 * <p>本类为纯函数实现，不依赖 Spring 上下文与持久化，便于穷尽单测。</p>
 */
public class ReleaseLifecyclePlanner {

    /**
     * 4.7 第一级防抖：单 Release 持续 0 实例达到该时长才允许 RETIRED。
     */
    public static final long DEFAULT_RETIREMENT_GRACE_MINUTES = 5L;

    public static final Duration DEFAULT_RETIREMENT_GRACE = Duration.ofMinutes(DEFAULT_RETIREMENT_GRACE_MINUTES);

    private final Duration retirementGrace;

    public ReleaseLifecyclePlanner() {
        this(DEFAULT_RETIREMENT_GRACE);
    }

    /**
     * @param retirementGrace 0 实例防抖时长，必须为正数
     */
    public ReleaseLifecyclePlanner(Duration retirementGrace) {
        if (retirementGrace == null || retirementGrace.isZero() || retirementGrace.isNegative()) {
            throw new IllegalArgumentException("retirementGrace 必须为正数");
        }
        this.retirementGrace = retirementGrace;
    }

    /**
     * 计算某 Service 在本轮同步中的 Release 状态迁移计划。
     *
     * @param state 该 Service 的实例观测（含部署系统标记的默认版本）
     * @param now   本轮同步时刻
     */
    public ReleaseLifecyclePlan plan(ServiceInstanceState state, LocalDateTime now) {
        if (state == null) {
            throw new BusinessException("Release 退役判定失败：ServiceInstanceState 不能为空");
        }
        if (now == null) {
            throw new BusinessException("Release 退役判定失败：同步时刻不能为空，服务：" + state.serviceId());
        }

        if (state.fullyDown()) {
            return new ReleaseLifecyclePlan(state.serviceId(), true, List.of(),
                    "环境停机保护：该 Service 全部 Release 均为 0 实例，暂停一切状态迁移");
        }

        List<ReleaseTransition> transitions = new ArrayList<>();
        for (ReleaseInstanceState release : state.releases()) {
            ReleaseTransition transition = planOne(state, release, now);
            if (transition != null) {
                transitions.add(transition);
            }
        }
        return new ReleaseLifecyclePlan(state.serviceId(), false, transitions, null);
    }

    private ReleaseTransition planOne(ServiceInstanceState state, ReleaseInstanceState release, LocalDateTime now) {
        String version = release.releaseVersion();
        ReleaseStatus current = release.currentStatus();

        boolean markedAsDefaultVersion = version.equals(state.defaultReleaseVersion());
        if (release.hasInstance() || markedAsDefaultVersion) {
            String reason = release.hasInstance()
                    ? "存在运行实例（" + release.instanceCount() + " 个）"
                    : "被部署系统标记为当前有效版本";

            if (current == ReleaseStatus.ACTIVE) {
                if (release.zeroInstanceSince() == null) {
                    return null;
                }
                return new ReleaseTransition(version, current, current, null, reason + "，清除防抖计时");
            }
            return new ReleaseTransition(version, current, current.transitionTo(ReleaseStatus.ACTIVE), null, reason);
        }

        if (current == ReleaseStatus.RETIRED) {
            return null;
        }

        LocalDateTime zeroInstanceSince = release.zeroInstanceSince();
        if (zeroInstanceSince == null) {
            return new ReleaseTransition(version, current, current, now, "开始 0 实例防抖计时");
        }
        if (!now.isBefore(zeroInstanceSince.plus(retirementGrace))) {
            return new ReleaseTransition(version, current, current.transitionTo(ReleaseStatus.RETIRED), null,
                    "持续 0 实例已满 " + retirementGrace.toMinutes() + " 分钟");
        }
        return null;
    }
}
