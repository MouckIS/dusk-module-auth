package com.dusk.module.auth.registry.lifecycle;

import com.dusk.common.core.exception.BusinessException;
import com.dusk.module.auth.registry.enums.PermissionStatus;
import com.dusk.module.auth.registry.enums.TransitionTrigger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link PermissionLifecycleEvaluator} 单元测试，覆盖《权限优化方案-整理版》
 * 4.3 状态机、4.8 孤立判定与 4.11 回滚语义。
 */
class PermissionLifecycleEvaluatorTest {

    private static final String PERMISSION = "system:user:delete";

    private final PermissionLifecycleEvaluator evaluator = new PermissionLifecycleEvaluator();

    @Test
    @DisplayName("仍被 ACTIVE Release 引用：保持 ACTIVE")
    void shouldStayActiveWhileUsed() {
        PermissionTransition transition = evaluator.evaluate(
                new PermissionEvaluationInput(PERMISSION, PermissionStatus.ACTIVE, true));

        assertThat(transition.changed()).isFalse();
        assertThat(transition.to()).isEqualTo(PermissionStatus.ACTIVE);
        assertThat(transition.trigger()).isEqualTo(TransitionTrigger.AUTO);
    }

    @Test
    @DisplayName("不再被任何 ACTIVE Release 引用：自动进入 ORPHANED")
    void shouldBecomeOrphanedWhenUnused() {
        PermissionTransition transition = evaluator.evaluate(
                new PermissionEvaluationInput(PERMISSION, PermissionStatus.ACTIVE, false));

        assertThat(transition.changed()).isTrue();
        assertThat(transition.from()).isEqualTo(PermissionStatus.ACTIVE);
        assertThat(transition.to()).isEqualTo(PermissionStatus.ORPHANED);
        assertThat(transition.reason()).contains("RolePermission 保留");
    }

    @Test
    @DisplayName("回滚重新被引用：ORPHANED 自动恢复为 ACTIVE")
    void shouldAutoRestoreOrphanedOnRollback() {
        PermissionTransition transition = evaluator.evaluate(
                new PermissionEvaluationInput(PERMISSION, PermissionStatus.ORPHANED, true));

        assertThat(transition.changed()).isTrue();
        assertThat(transition.to()).isEqualTo(PermissionStatus.ACTIVE);
        assertThat(transition.trigger()).isEqualTo(TransitionTrigger.AUTO);
    }

    @Test
    @DisplayName("仍无引用时 ORPHANED 保持，等待管理员处置")
    void shouldKeepOrphanedWhileStillUnused() {
        PermissionTransition transition = evaluator.evaluate(
                new PermissionEvaluationInput(PERMISSION, PermissionStatus.ORPHANED, false));

        assertThat(transition.changed()).isFalse();
        assertThat(transition.to()).isEqualTo(PermissionStatus.ORPHANED);
    }

    @Test
    @DisplayName("DISABLED 只进不出：即使重新被引用，自动链路也不恢复")
    void shouldNeverAutoRestoreDisabled() {
        PermissionTransition used = evaluator.evaluate(
                new PermissionEvaluationInput(PERMISSION, PermissionStatus.DISABLED, true));
        PermissionTransition unused = evaluator.evaluate(
                new PermissionEvaluationInput(PERMISSION, PermissionStatus.DISABLED, false));

        assertThat(used.changed()).isFalse();
        assertThat(used.to()).isEqualTo(PermissionStatus.DISABLED);
        assertThat(unused.changed()).isFalse();
        assertThat(unused.reason()).contains("自动链路不改变该状态");
    }

    @Test
    @DisplayName("管理员手工恢复：DISABLED -> ACTIVE，触发来源为 MANUAL")
    void shouldRestoreManually() {
        PermissionTransition transition = evaluator.restoreManually(PERMISSION, PermissionStatus.DISABLED);

        assertThat(transition.to()).isEqualTo(PermissionStatus.ACTIVE);
        assertThat(transition.trigger()).isEqualTo(TransitionTrigger.MANUAL);
    }

    @Test
    @DisplayName("ORPHANED 没有人工恢复通路，且对已 ACTIVE 的恢复是空操作")
    void shouldRejectManualRestoreOfOrphanedAndBeNoOpForActive() {
        assertThatThrownBy(() -> evaluator.restoreManually(PERMISSION, PermissionStatus.ORPHANED))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无需也不能手工恢复为 ACTIVE");

        PermissionTransition noOp = evaluator.restoreManually(PERMISSION, PermissionStatus.ACTIVE);
        assertThat(noOp.changed()).isFalse();
        assertThat(noOp.reason()).contains("无需恢复");
    }

    @Test
    @DisplayName("管理员手工禁用：仅 ORPHANED 可禁用，ACTIVE 直接拒绝")
    void shouldDisableOnlyOrphanedManually() {
        PermissionTransition transition = evaluator.disableManually(PERMISSION, PermissionStatus.ORPHANED);
        assertThat(transition.to()).isEqualTo(PermissionStatus.DISABLED);
        assertThat(transition.trigger()).isEqualTo(TransitionTrigger.MANUAL);

        assertThatThrownBy(() -> evaluator.disableManually(PERMISSION, PermissionStatus.ACTIVE))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("非法的 Permission 状态迁移");

        PermissionTransition noOp = evaluator.disableManually(PERMISSION, PermissionStatus.DISABLED);
        assertThat(noOp.changed()).isFalse();
        assertThat(noOp.reason()).contains("无需变更");
    }

    @Test
    @DisplayName("手工接口的参数校验")
    void shouldValidateManualInputs() {
        assertThatThrownBy(() -> evaluator.restoreManually(" ", PermissionStatus.DISABLED))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("权限码不能为空");
        assertThatThrownBy(() -> evaluator.restoreManually(PERMISSION, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("当前状态不能为空");
        assertThatThrownBy(() -> evaluator.disableManually(null, PermissionStatus.ORPHANED))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> evaluator.disableManually(PERMISSION, null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("评估输入与 null 校验")
    void shouldValidateEvaluationInput() {
        assertThatThrownBy(() -> evaluator.evaluate(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("评估输入不能为空");

        assertThatThrownBy(() -> new PermissionEvaluationInput(" ", PermissionStatus.ACTIVE, true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PermissionEvaluationInput(PERMISSION, null, true))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
