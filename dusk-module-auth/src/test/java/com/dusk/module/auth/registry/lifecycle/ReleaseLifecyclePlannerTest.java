package com.dusk.module.auth.registry.lifecycle;

import com.dusk.common.core.exception.BusinessException;
import com.dusk.module.auth.registry.enums.ReleaseStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link ReleaseLifecyclePlanner} 单元测试，覆盖《权限优化方案-整理版》4.7 的两级防抖。
 */
class ReleaseLifecyclePlannerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 7, 12, 0, 0);
    private static final String SERVICE_ID = "dusk-module-order";

    private final ReleaseLifecyclePlanner planner = new ReleaseLifecyclePlanner();

    private ServiceInstanceState state(String defaultVersion, ReleaseInstanceState... releases) {
        return new ServiceInstanceState(SERVICE_ID, List.of(releases), defaultVersion);
    }

    /**
     * 追加一个「另有实例在线」的陪跑 Release。
     *
     * <p>4.7 的环境停机保护以「该 Service 全部 Release 均 0 实例」为触发条件，
     * 因此单 Release 0 实例的场景会被判定为停机而冻结；要验证退役倒计时本身，
     * 需要一个在线 Release 让该服务不满足「全部 0 实例」。</p>
     */
    private ServiceInstanceState stateWithLiveCompanion(ReleaseInstanceState... releases) {
        List<ReleaseInstanceState> all = new java.util.ArrayList<>();
        all.add(new ReleaseInstanceState("9.9.9", ReleaseStatus.ACTIVE, 1, null));
        all.addAll(List.of(releases));
        return new ServiceInstanceState(SERVICE_ID, all, null);
    }

    @Test
    @DisplayName("环境停机保护：全部 Release 均为 0 实例时冻结，不产生任何状态迁移")
    void shouldFreezeWhenServiceFullyDown() {
        ReleaseLifecyclePlan plan = planner.plan(state(null,
                new ReleaseInstanceState("1.0.0", ReleaseStatus.ACTIVE, 0, NOW.minusMinutes(60)),
                new ReleaseInstanceState("1.1.0", ReleaseStatus.ACTIVE, 0, NOW.minusMinutes(60))), NOW);

        assertThat(plan.frozen()).isTrue();
        assertThat(plan.hasChanges()).isFalse();
        assertThat(plan.freezeReason()).contains("环境停机保护");
    }

    @Test
    @DisplayName("环境停机保护：冻结期间即使计时早已超阈值也不退役")
    void shouldNotRetireDuringFreeze() {
        ReleaseLifecyclePlan plan = planner.plan(state(null,
                new ReleaseInstanceState("1.0.0", ReleaseStatus.REGISTERED, 0, NOW.minusDays(1))), NOW);

        assertThat(plan.frozen()).isTrue();
        assertThat(plan.transitions()).isEmpty();
        assertThat(plan.hasRetirement()).isFalse();
    }

    @Test
    @DisplayName("存在运行实例的 REGISTERED Release 进入 ACTIVE")
    void shouldActivateRegisteredReleaseWithInstance() {
        ReleaseLifecyclePlan plan = planner.plan(state(null,
                new ReleaseInstanceState("1.2.0", ReleaseStatus.REGISTERED, 2, null)), NOW);

        assertThat(plan.frozen()).isFalse();
        ReleaseTransition transition = plan.transitions().getFirst();
        assertThat(transition.statusChanged()).isTrue();
        assertThat(transition.to()).isEqualTo(ReleaseStatus.ACTIVE);
        assertThat(transition.zeroInstanceSince()).isNull();
    }

    @Test
    @DisplayName("回滚：已 RETIRED 的 Release 重新出现实例则回到 ACTIVE")
    void shouldReactivateRetiredReleaseOnRollback() {
        ReleaseLifecyclePlan plan = planner.plan(state(null,
                new ReleaseInstanceState("1.2.0", ReleaseStatus.RETIRED, 1, null)), NOW);

        ReleaseTransition transition = plan.transitions().getFirst();
        assertThat(transition.from()).isEqualTo(ReleaseStatus.RETIRED);
        assertThat(transition.to()).isEqualTo(ReleaseStatus.ACTIVE);
    }

    @Test
    @DisplayName("ACTIVE 且存在实例时清除残留的 0 实例计时")
    void shouldClearZeroInstanceTimerWhenInstanceAppears() {
        ReleaseLifecyclePlan plan = planner.plan(state(null,
                new ReleaseInstanceState("1.2.0", ReleaseStatus.ACTIVE, 3, NOW.minusMinutes(2))), NOW);

        ReleaseTransition transition = plan.transitions().getFirst();
        assertThat(transition.statusChanged()).isFalse();
        assertThat(transition.zeroInstanceSince()).isNull();
        assertThat(transition.reason()).contains("清除防抖计时");
    }

    @Test
    @DisplayName("ACTIVE 且状态与计时都无需变更时不产生任何迁移")
    void shouldProduceNoTransitionWhenNothingChanges() {
        ReleaseLifecyclePlan plan = planner.plan(state(null,
                new ReleaseInstanceState("1.2.0", ReleaseStatus.ACTIVE, 3, null)), NOW);

        assertThat(plan.hasChanges()).isFalse();
    }

    @Test
    @DisplayName("ACTIVE 开始 0 实例：第一轮只记录计时起点，不退役")
    void shouldStartRetirementCountdown() {
        ReleaseLifecyclePlan plan = planner.plan(stateWithLiveCompanion(
                new ReleaseInstanceState("1.0.0", ReleaseStatus.ACTIVE, 0, null)), NOW);

        ReleaseTransition transition = plan.transitions().getFirst();
        assertThat(transition.statusChanged()).isFalse();
        assertThat(transition.zeroInstanceSince()).isEqualTo(NOW);
        assertThat(transition.reason()).contains("防抖计时");
        assertThat(plan.hasRetirement()).isFalse();
    }

    @Test
    @DisplayName("0 实例未满 5 分钟不退役")
    void shouldNotRetireBeforeGraceElapsed() {
        ReleaseLifecyclePlan plan = planner.plan(stateWithLiveCompanion(
                new ReleaseInstanceState("1.0.0", ReleaseStatus.ACTIVE, 0, NOW.minusMinutes(5).plusSeconds(1))), NOW);

        assertThat(plan.hasChanges()).isFalse();
    }

    @Test
    @DisplayName("0 实例恰好满 5 分钟即退役（防抖边界取闭区间）")
    void shouldRetireAtGraceBoundary() {
        ReleaseLifecyclePlan plan = planner.plan(stateWithLiveCompanion(
                new ReleaseInstanceState("1.0.0", ReleaseStatus.ACTIVE, 0, NOW.minusMinutes(5))), NOW);

        ReleaseTransition transition = plan.transitions().getFirst();
        assertThat(transition.to()).isEqualTo(ReleaseStatus.RETIRED);
        assertThat(plan.hasRetirement()).isTrue();
    }

    @Test
    @DisplayName("已 RETIRED 且仍 0 实例时保持不动")
    void shouldKeepRetiredReleaseUntouched() {
        ReleaseLifecyclePlan plan = planner.plan(stateWithLiveCompanion(
                new ReleaseInstanceState("1.0.0", ReleaseStatus.RETIRED, 0, null)), NOW);

        assertThat(plan.hasChanges()).isFalse();
    }

    @Test
    @DisplayName("已知边界：只有单个 Release 的服务全 0 实例时被环境停机保护冻结，不会自动退役")
    void singleReleaseServiceFullyDownIsFrozen() {
        ReleaseLifecyclePlan plan = planner.plan(state(null,
                new ReleaseInstanceState("1.0.0", ReleaseStatus.ACTIVE, 0, NOW.minusDays(7))), NOW);

        assertThat(plan.frozen()).isTrue();
        assertThat(plan.hasRetirement()).isFalse();
        assertThat(plan.transitions()).isEmpty();
    }

    @Test
    @DisplayName("0 实例但被部署系统标记为当前有效版本时保持 ACTIVE")
    void shouldKeepDefaultVersionActiveWithoutInstance() {
        ReleaseLifecyclePlan plan = planner.plan(state("1.1.0",
                new ReleaseInstanceState("1.1.0", ReleaseStatus.REGISTERED, 0, null),
                new ReleaseInstanceState("1.0.0", ReleaseStatus.ACTIVE, 1, null)), NOW);

        ReleaseTransition activated = plan.transitions().stream()
                .filter(transition -> "1.1.0".equals(transition.releaseVersion()))
                .findFirst()
                .orElseThrow();
        assertThat(activated.to()).isEqualTo(ReleaseStatus.ACTIVE);
        assertThat(activated.reason()).contains("当前有效版本");
        assertThat(plan.frozen()).isFalse();
    }

    @Test
    @DisplayName("REGISTERED 且在拿到实例前被撤下：允许迁移到 RETIRED（4.7 的必要扩展）")
    void shouldRetireRegisteredReleaseWithoutInstance() {
        ReleaseLifecyclePlan plan = planner.plan(state(null,
                new ReleaseInstanceState("1.9.0", ReleaseStatus.REGISTERED, 0, NOW.minusMinutes(10)),
                new ReleaseInstanceState("1.8.0", ReleaseStatus.ACTIVE, 1, null)), NOW);

        ReleaseTransition retired = plan.transitions().stream()
                .filter(transition -> "1.9.0".equals(transition.releaseVersion()))
                .findFirst()
                .orElseThrow();
        assertThat(retired.to()).isEqualTo(ReleaseStatus.RETIRED);
    }

    @Test
    @DisplayName("防抖时长可配置")
    void shouldHonorCustomGrace() {
        ReleaseLifecyclePlanner shortGrace = new ReleaseLifecyclePlanner(Duration.ofSeconds(30));

        ReleaseLifecyclePlan plan = shortGrace.plan(state(null,
                new ReleaseInstanceState("1.0.0", ReleaseStatus.ACTIVE, 0, NOW.minusSeconds(31)),
                new ReleaseInstanceState("1.1.0", ReleaseStatus.ACTIVE, 1, null)), NOW);

        assertThat(plan.hasRetirement()).isTrue();
    }

    @Test
    @DisplayName("非法防抖时长直接失败")
    void shouldRejectInvalidGrace() {
        assertThatThrownBy(() -> new ReleaseLifecyclePlanner(Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("必须为正数");
        assertThatThrownBy(() -> new ReleaseLifecyclePlanner(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("null 入参直接失败")
    void shouldRejectNullInput() {
        assertThatThrownBy(() -> planner.plan(null, NOW))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ServiceInstanceState 不能为空");
        assertThatThrownBy(() -> planner.plan(state(null), null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("同步时刻不能为空");
    }

    @Test
    @DisplayName("无任何已知 Release 的服务视为全停机，冻结")
    void shouldFreezeServiceWithoutKnownRelease() {
        ReleaseLifecyclePlan plan = planner.plan(state(null), NOW);

        assertThat(plan.frozen()).isTrue();
        assertThat(plan.hasChanges()).isFalse();
    }
}
