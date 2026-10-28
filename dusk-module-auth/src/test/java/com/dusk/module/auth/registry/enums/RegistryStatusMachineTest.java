package com.dusk.module.auth.registry.enums;

import com.dusk.common.core.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 三套状态机的迁移规则测试，对应《权限优化方案-整理版》4.1 / 4.3 / 4.7 / 4.9 / 4.11。
 *
 * <p>这些规则是「代码发布不得篡改管理员授权决策」这一约束的强制执行点，
 * 因此对非法迁移逐个断言，确保回归时立刻暴露。</p>
 */
class RegistryStatusMachineTest {

    @Nested
    @DisplayName("ResourceStatus（4.1 / 4.9 / 4.11）")
    class ResourceStatusTest {

        @Test
        @DisplayName("合法迁移：换绑挂起、拒绝、废弃、回滚恢复")
        void shouldAllowLegalTransitions() {
            assertThat(ResourceStatus.ACTIVE.canTransitionTo(ResourceStatus.PENDING_REBIND)).isTrue();
            assertThat(ResourceStatus.ACTIVE.canTransitionTo(ResourceStatus.DEPRECATED)).isTrue();
            assertThat(ResourceStatus.PENDING_REBIND.canTransitionTo(ResourceStatus.ACTIVE)).isTrue();
            assertThat(ResourceStatus.PENDING_REBIND.canTransitionTo(ResourceStatus.REBIND_REJECTED)).isTrue();
            assertThat(ResourceStatus.REBIND_REJECTED.canTransitionTo(ResourceStatus.PENDING_REBIND)).isTrue();
            assertThat(ResourceStatus.REBIND_REJECTED.canTransitionTo(ResourceStatus.ACTIVE)).isTrue();
            assertThat(ResourceStatus.DEPRECATED.canTransitionTo(ResourceStatus.ACTIVE)).isTrue();
            assertThat(ResourceStatus.DEPRECATED.canTransitionTo(ResourceStatus.PENDING_REBIND)).isTrue();
        }

        @Test
        @DisplayName("非法迁移：ACTIVE 不能直接进入 REBIND_REJECTED，DEPRECATED 不能进入 REBIND_REJECTED")
        void shouldRejectIllegalTransitions() {
            assertThat(ResourceStatus.ACTIVE.canTransitionTo(ResourceStatus.REBIND_REJECTED)).isFalse();
            assertThat(ResourceStatus.DEPRECATED.canTransitionTo(ResourceStatus.REBIND_REJECTED)).isFalse();
            assertThatThrownBy(() -> ResourceStatus.DEPRECATED.transitionTo(ResourceStatus.REBIND_REJECTED))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("非法的 Resource 状态迁移");
        }

        @Test
        @DisplayName("自身迁移是幂等空操作")
        void selfTransitionShouldBeNoOp() {
            assertThat(ResourceStatus.ACTIVE.canTransitionTo(ResourceStatus.ACTIVE)).isTrue();
            assertThat(ResourceStatus.ACTIVE.transitionTo(ResourceStatus.ACTIVE)).isEqualTo(ResourceStatus.ACTIVE);
        }

        @Test
        @DisplayName("换绑期间仍参与运行时判定（4.9 不自动生效）")
        void shouldStayEffectiveForRuntimeDuringRebind() {
            assertThat(ResourceStatus.ACTIVE.effectiveForRuntime()).isTrue();
            assertThat(ResourceStatus.PENDING_REBIND.effectiveForRuntime()).isTrue();
            assertThat(ResourceStatus.REBIND_REJECTED.effectiveForRuntime()).isTrue();
            assertThat(ResourceStatus.DEPRECATED.effectiveForRuntime()).isFalse();
        }

        @Test
        @DisplayName("null 目标状态被拒绝")
        void shouldRejectNullTarget() {
            assertThat(ResourceStatus.ACTIVE.canTransitionTo(null)).isFalse();
            assertThatThrownBy(() -> ResourceStatus.ACTIVE.transitionTo(null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("迁移目标不能为空");
        }

        @Test
        @DisplayName("后继状态集合不可变")
        void allowedTargetsShouldBeImmutable() {
            assertThatThrownBy(() -> ResourceStatus.ACTIVE.allowedTargets().add(ResourceStatus.ACTIVE))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("ReleaseStatus（4.5 / 4.7）")
    class ReleaseStatusTest {

        @Test
        @DisplayName("合法迁移：注册→生效→退役→回滚生效")
        void shouldAllowLegalTransitions() {
            assertThat(ReleaseStatus.REGISTERED.canTransitionTo(ReleaseStatus.ACTIVE)).isTrue();
            assertThat(ReleaseStatus.ACTIVE.canTransitionTo(ReleaseStatus.RETIRED)).isTrue();
            assertThat(ReleaseStatus.RETIRED.canTransitionTo(ReleaseStatus.ACTIVE)).isTrue();
        }

        @Test
        @DisplayName("REGISTERED -> RETIRED 为必要扩展：拿到实例前被撤下的 Release 也能退役")
        void registeredMayRetire() {
            assertThat(ReleaseStatus.REGISTERED.canTransitionTo(ReleaseStatus.RETIRED)).isTrue();
        }

        @Test
        @DisplayName("非法迁移：不能回退到 REGISTERED")
        void shouldRejectBackwardTransitions() {
            assertThat(ReleaseStatus.ACTIVE.canTransitionTo(ReleaseStatus.REGISTERED)).isFalse();
            assertThat(ReleaseStatus.RETIRED.canTransitionTo(ReleaseStatus.REGISTERED)).isFalse();
            assertThatThrownBy(() -> ReleaseStatus.ACTIVE.transitionTo(ReleaseStatus.REGISTERED))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("非法的 Release 状态迁移");
        }

        @Test
        @DisplayName("只有 ACTIVE 的 Release 支撑其 Permission 的生效性")
        void onlyActiveKeepsPermissionsAlive() {
            assertThat(ReleaseStatus.ACTIVE.keepsPermissionsAlive()).isTrue();
            assertThat(ReleaseStatus.REGISTERED.keepsPermissionsAlive()).isFalse();
            assertThat(ReleaseStatus.RETIRED.keepsPermissionsAlive()).isFalse();
        }

        @Test
        @DisplayName("null 目标状态被拒绝")
        void shouldRejectNullTarget() {
            assertThat(ReleaseStatus.REGISTERED.canTransitionTo(null)).isFalse();
            assertThatThrownBy(() -> ReleaseStatus.REGISTERED.transitionTo(null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("迁移目标不能为空");
        }
    }

    @Nested
    @DisplayName("PermissionStatus（4.3 / 4.8 / 4.11）")
    class PermissionStatusTest {

        @Test
        @DisplayName("自动链路：ACTIVE -> ORPHANED、ORPHANED -> ACTIVE")
        void autoTransitions() {
            assertThat(PermissionStatus.ACTIVE.transitionTo(PermissionStatus.ORPHANED, TransitionTrigger.AUTO))
                    .isEqualTo(PermissionStatus.ORPHANED);
            assertThat(PermissionStatus.ORPHANED.transitionTo(PermissionStatus.ACTIVE, TransitionTrigger.AUTO))
                    .isEqualTo(PermissionStatus.ACTIVE);
        }

        @Test
        @DisplayName("人工链路：ORPHANED -> DISABLED、DISABLED -> ACTIVE")
        void manualTransitions() {
            assertThat(PermissionStatus.ORPHANED.transitionTo(PermissionStatus.DISABLED, TransitionTrigger.MANUAL))
                    .isEqualTo(PermissionStatus.DISABLED);
            assertThat(PermissionStatus.DISABLED.transitionTo(PermissionStatus.ACTIVE, TransitionTrigger.MANUAL))
                    .isEqualTo(PermissionStatus.ACTIVE);
        }

        @Test
        @DisplayName("DISABLED -> ACTIVE 严禁自动触发：部署动作不得静默回滚管理员决策")
        void disabledMustNotAutoRestore() {
            assertThat(PermissionStatus.DISABLED.canTransitionTo(PermissionStatus.ACTIVE, TransitionTrigger.AUTO)).isFalse();
            assertThatThrownBy(() -> PermissionStatus.DISABLED.transitionTo(PermissionStatus.ACTIVE, TransitionTrigger.AUTO))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("非法的 Permission 状态迁移");
        }

        @Test
        @DisplayName("自动/人工链路不得互换：ACTIVE->ORPHANED 不能人工、ORPHANED->ACTIVE 不能人工")
        void shouldNotSwapTriggerSemantics() {
            assertThat(PermissionStatus.ACTIVE.canTransitionTo(PermissionStatus.ORPHANED, TransitionTrigger.MANUAL)).isFalse();
            assertThat(PermissionStatus.ORPHANED.canTransitionTo(PermissionStatus.ACTIVE, TransitionTrigger.MANUAL)).isFalse();
            assertThat(PermissionStatus.ORPHANED.canTransitionTo(PermissionStatus.DISABLED, TransitionTrigger.AUTO)).isFalse();
        }

        @Test
        @DisplayName("不存在 ACTIVE -> DISABLED 与 DISABLED -> ORPHANED 的直接通路")
        void shouldHaveNoShortcutTransitions() {
            for (TransitionTrigger trigger : TransitionTrigger.values()) {
                assertThat(PermissionStatus.ACTIVE.canTransitionTo(PermissionStatus.DISABLED, trigger)).isFalse();
                assertThat(PermissionStatus.DISABLED.canTransitionTo(PermissionStatus.ORPHANED, trigger)).isFalse();
            }
        }

        @Test
        @DisplayName("ORPHANED 仍参与运行时判定（授权关系保留）")
        void orphanedShouldStayEffective() {
            assertThat(PermissionStatus.ACTIVE.effectiveForRuntime()).isTrue();
            assertThat(PermissionStatus.ORPHANED.effectiveForRuntime()).isTrue();
            assertThat(PermissionStatus.DISABLED.effectiveForRuntime()).isFalse();
        }

        @Test
        @DisplayName("按触发来源查询后继状态，null 触发来源返回空集合")
        void allowedTargetsByTrigger() {
            assertThat(PermissionStatus.ACTIVE.allowedTargets(TransitionTrigger.AUTO))
                    .containsExactly(PermissionStatus.ORPHANED);
            assertThat(PermissionStatus.ACTIVE.allowedTargets(TransitionTrigger.MANUAL)).isEmpty();
            assertThat(PermissionStatus.DISABLED.allowedTargets(TransitionTrigger.MANUAL))
                    .containsExactly(PermissionStatus.ACTIVE);
            assertThat(PermissionStatus.ACTIVE.allowedTargets(null)).isEmpty();
        }

        @Test
        @DisplayName("null 参数被拒绝")
        void shouldRejectNullArguments() {
            assertThat(PermissionStatus.ACTIVE.canTransitionTo(null, TransitionTrigger.AUTO)).isFalse();
            assertThat(PermissionStatus.ACTIVE.canTransitionTo(PermissionStatus.ORPHANED, null)).isFalse();
            assertThatThrownBy(() -> PermissionStatus.ACTIVE.transitionTo(null, TransitionTrigger.AUTO))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("迁移目标不能为空");
            assertThatThrownBy(() -> PermissionStatus.ACTIVE.transitionTo(PermissionStatus.ORPHANED, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("触发来源不能为空");
        }
    }

    @Nested
    @DisplayName("ResourceDiffType 与 TransitionTrigger")
    class DiffTypeTest {

        @Test
        @DisplayName("Diff 类型齐备且带中文展示名")
        void diffTypesAreComplete() {
            assertThat(ResourceDiffType.values()).hasSize(4);
            assertThat(ResourceDiffType.REBIND.getDisplayName()).isEqualTo("换绑");
            assertThat(ResourceDiffType.ADD.getDescription()).isNotBlank();
        }

        @Test
        @DisplayName("触发来源只有自动与人工两种")
        void triggers() {
            assertThat(TransitionTrigger.values()).containsExactly(TransitionTrigger.AUTO, TransitionTrigger.MANUAL);
            assertThat(TransitionTrigger.MANUAL.getDisplayName()).isEqualTo("人工");
        }
    }
}
