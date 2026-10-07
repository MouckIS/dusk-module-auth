package com.dusk.module.auth.registry.lifecycle;

import com.dusk.common.core.exception.BusinessException;
import com.dusk.module.auth.registry.enums.PermissionStatus;
import com.dusk.module.auth.registry.enums.TransitionTrigger;

/**
 * Permission 生命周期判定，见《权限优化方案-整理版》4.3 / 4.8 / 4.11。
 *
 * <p>判定规则：</p>
 * <ul>
 *   <li>{@code ACTIVE → ORPHANED}：不再被任何 ACTIVE Release 引用（4.8）；</li>
 *   <li>{@code ORPHANED → ACTIVE}：回滚后重新被引用，<b>自动</b>恢复，不需要重建 Permission（4.11）；</li>
 *   <li>{@code DISABLED}：只进不出自动链路。管理员在抖动期的禁用是人工安全决策，
 *       绝不能被代码部署静默回滚（4.3），恢复必须由 {@link #restoreManually} 显式触发。</li>
 * </ul>
 *
 * <p><b>关于 4.7 的环境停机保护</b>：该保护通过「冻结 Release 状态迁移」实现，
 * 冻结期间 Release 保持 ACTIVE，因此本评估器的 {@code usedByActiveRelease} 自然为 {@code true}，
 * 不会批量产生 ORPHANED。这里不再重复引入冻结参数，避免两处规则相互矛盾。</p>
 *
 * <p>本类为纯函数实现，不依赖 Spring 上下文与持久化，便于穷尽单测。</p>
 */
public class PermissionLifecycleEvaluator {

    /**
     * 按自动链路推演目标状态。
     *
     * <p>返回值中 {@code from == to} 表示无需变更，调用方应跳过写库。
     * 状态迁移经 {@code PermissionStatus.transitionTo} 校验，规则错误会立即抛错而不是静默改坏数据。</p>
     */
    public PermissionTransition evaluate(PermissionEvaluationInput input) {
        if (input == null) {
            throw new BusinessException("Permission 生命周期判定失败：评估输入不能为空");
        }

        String code = input.permissionCode();
        PermissionStatus current = input.currentStatus();

        if (current == PermissionStatus.DISABLED) {
            return new PermissionTransition(code, current, current, TransitionTrigger.AUTO,
                    "已由管理员手工禁用，自动链路不改变该状态");
        }

        if (input.usedByActiveRelease()) {
            if (current == PermissionStatus.ORPHANED) {
                return new PermissionTransition(code, current,
                        current.transitionTo(PermissionStatus.ACTIVE, TransitionTrigger.AUTO),
                        TransitionTrigger.AUTO, "重新被 ACTIVE Release 引用，随回滚自动恢复");
            }
            return new PermissionTransition(code, current, current, TransitionTrigger.AUTO,
                    "仍被 ACTIVE Release 引用，保持生效");
        }

        if (current == PermissionStatus.ACTIVE) {
            return new PermissionTransition(code, current,
                    current.transitionTo(PermissionStatus.ORPHANED, TransitionTrigger.AUTO),
                    TransitionTrigger.AUTO, "不再被任何 ACTIVE Release 引用，RolePermission 保留");
        }

        return new PermissionTransition(code, current, current, TransitionTrigger.AUTO,
                "仍无 ACTIVE Release 引用，等待管理员决定保留或禁用");
    }

    /**
     * 管理员手工恢复：{@code DISABLED → ACTIVE}。
     *
     * <p>这是 {@code DISABLED} 唯一的出边（4.3），且只能由人工触发。</p>
     *
     * <p>注意 {@code ORPHANED} 没有人工恢复通路：ORPHANED 的含义是「当前没有任何有效 API 使用它」，
     * 管理员对它只有「保留（不变）」或「禁用」两种处置，重新变 ACTIVE 必须由资源重新被引用自动触发（4.8）。</p>
     *
     * @throws BusinessException 当状态无法合法迁移到 ACTIVE 时
     */
    public PermissionTransition restoreManually(String permissionCode, PermissionStatus currentStatus) {
        if (permissionCode == null || permissionCode.isBlank()) {
            throw new BusinessException("Permission 手工恢复失败：权限码不能为空");
        }
        if (currentStatus == null) {
            throw new BusinessException("Permission 手工恢复失败：当前状态不能为空，权限：" + permissionCode);
        }

        String code = permissionCode.trim();
        if (currentStatus == PermissionStatus.ACTIVE) {
            return new PermissionTransition(code, currentStatus, currentStatus, TransitionTrigger.MANUAL,
                    "已是生效状态，无需恢复");
        }
        if (currentStatus == PermissionStatus.ORPHANED) {
            throw new BusinessException("Permission " + code
                    + " 当前为 ORPHANED（无任何有效 API 使用），无需也不能手工恢复为 ACTIVE："
                    + "管理员只能选择保留（不变）或禁用，重新生效由资源被重新引用自动触发");
        }
        return new PermissionTransition(code, currentStatus,
                currentStatus.transitionTo(PermissionStatus.ACTIVE, TransitionTrigger.MANUAL),
                TransitionTrigger.MANUAL, "管理员手工恢复");
    }

    /**
     * 管理员手工禁用：仅 {@code ORPHANED → DISABLED}（4.3 状态图唯一入边）。
     *
     * @throws BusinessException 当权限并非 ORPHANED 时——避免管理员误禁用仍在生效的权限
     */
    public PermissionTransition disableManually(String permissionCode, PermissionStatus currentStatus) {
        if (permissionCode == null || permissionCode.isBlank()) {
            throw new BusinessException("Permission 手工禁用失败：权限码不能为空");
        }
        if (currentStatus == null) {
            throw new BusinessException("Permission 手工禁用失败：当前状态不能为空，权限：" + permissionCode);
        }

        String code = permissionCode.trim();
        if (currentStatus == PermissionStatus.DISABLED) {
            return new PermissionTransition(code, currentStatus, currentStatus, TransitionTrigger.MANUAL,
                    "已是禁用状态，无需变更");
        }
        return new PermissionTransition(code, currentStatus,
                currentStatus.transitionTo(PermissionStatus.DISABLED, TransitionTrigger.MANUAL),
                TransitionTrigger.MANUAL, "管理员确认禁用孤立权限");
    }
}
