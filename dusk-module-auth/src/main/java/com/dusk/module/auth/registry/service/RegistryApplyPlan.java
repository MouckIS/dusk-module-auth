package com.dusk.module.auth.registry.service;

import com.dusk.module.auth.registry.diff.ResourceDiffResult;
import com.dusk.module.auth.registry.enums.ResourceDiffType;

import java.util.List;
import java.util.Set;

/**
 * 一次注册的落库计划，见《权限优化方案-整理版》3.4。
 *
 * <p>由 {@link RegistryApplyPlanner} 纯计算产出，事务内的持久化由
 * {@code IResourceRegistryService} 实现负责。把「怎么算」与「怎么写」分开，
 * 是为了让 4.1/4.9 的状态判定能被穷尽单测——那部分是最容易出错、也最难在集成环境复现的逻辑。</p>
 *
 * @param diff                Diff 结果，用于生成注册报告与告警（4.1）
 * @param mutations           需要施加到 Resource 上的变更（已按类型 + 路径排序，输出稳定）
 * @param touchedPermissions  本次注册触及的权限码集合：包括生效绑定、换绑候选与被废弃资源原有绑定，
 *                            但不含匿名字面量 {@code ANONYMOUS}。调用方据此做「确保存在 + 生效性重算」（4.8）
 */
public record RegistryApplyPlan(
        ResourceDiffResult diff,
        List<ResourceMutation> mutations,
        Set<String> touchedPermissions) {

    public RegistryApplyPlan {
        diff = diff == null ? ResourceDiffResult.empty() : diff;
        mutations = mutations == null ? List.of() : List.copyOf(mutations);
        touchedPermissions = touchedPermissions == null ? Set.of() : Set.copyOf(touchedPermissions);
    }

    /**
     * 本次注册是否没有任何需要落库的变化（幂等命中）。
     */
    public boolean isEmpty() {
        return mutations.isEmpty() && touchedPermissions.isEmpty();
    }

    public List<ResourceMutation> ofType(ResourceDiffType type) {
        return mutations.stream().filter(mutation -> mutation.type() == type).toList();
    }
}
