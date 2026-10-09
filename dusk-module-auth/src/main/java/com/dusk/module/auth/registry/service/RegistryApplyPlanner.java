package com.dusk.module.auth.registry.service;

import com.dusk.common.core.auth.registry.ResourceSnapshot;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.module.auth.registry.diff.PermissionRoleLookup;
import com.dusk.module.auth.registry.diff.ResourceBinding;
import com.dusk.module.auth.registry.diff.ResourceDiffEngine;
import com.dusk.module.auth.registry.diff.ResourceDiffEntry;
import com.dusk.module.auth.registry.diff.ResourceDiffResult;
import com.dusk.module.auth.registry.diff.ResourceKey;
import com.dusk.module.auth.registry.enums.ResourceDiffType;
import com.dusk.module.auth.registry.enums.ResourceStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 由「完整快照 + 既有资源」推导落库计划，见《权限优化方案-整理版》3.4 / 4.1 / 4.9 / 4.11。
 *
 * <p><b>本类为纯函数</b>：不依赖 Spring 上下文与持久化，输入输出都是不可变值对象，
 * 因此 4.1 的四类 Diff、4.9 的换绑挂起、4.11 的回滚恢复都能脱离数据库穷尽单测——
 * 这三处正是最容易出错、也最难在集成环境复现的逻辑。</p>
 *
 * <h3>两个集合的区别（最容易搞错的地方）</h3>
 * <ul>
 *   <li>{@code existingResources}：<b>目标 Release 自己已有的资源</b>，决定「新建 / 更新 / 废弃」；</li>
 *   <li>{@code baseBindings}：<b>Diff 基准</b>。首次注册一个新 Release 时取「上一个 Release 的资源」，
 *       用于识别 REMOVE 候选与 REBIND（4.5：旧 Release 的独有资源在它退役前必须继续生效，
 *       不能因为新版本没上报就被废弃）。</li>
 * </ul>
 *
 * <p>由此得到一个重要推论：<b>「新 Release 的代表集」就是快照本身，而不是 Diff 结果</b>。
 * 新 Release 与上一版本完全相同的接口不会出现在 Diff 里，但它必须在本 Release 下落一行资源，
 * 否则该版本上线后这些接口在运行映射中缺失（5.7）。因此本类是「按快照物化 + 叠加换绑保护」，
 * 而不是「照着 Diff 逐条打补丁」。</p>
 */
public class RegistryApplyPlanner {

    /**
     * 输出顺序稳定：先按变更类型，再按 {@code method + path}，便于日志比对与测试断言。
     */
    private static final Comparator<ResourceMutation> MUTATION_ORDER =
            Comparator.comparing((ResourceMutation mutation) -> mutation.type().ordinal())
                    .thenComparing(ResourceMutation::method)
                    .thenComparing(ResourceMutation::path);

    private final ResourceDiffEngine diffEngine;

    public RegistryApplyPlanner() {
        this(new ResourceDiffEngine());
    }

    public RegistryApplyPlanner(ResourceDiffEngine diffEngine) {
        this.diffEngine = Objects.requireNonNull(diffEngine, "diffEngine");
    }

    /**
     * 生成落库计划。
     *
     * @param existingResources 目标 Release 已有资源；首次注册时为空集合
     * @param baseBindings      Diff 基准绑定集合；目标 Release 已注册过时等于
     *                          {@code existingResources} 的绑定集合
     * @param snapshot          SDK 上报的完整快照
     * @param roleLookup        换绑影响面查询；为 {@code null} 时影响面为空清单
     */
    public RegistryApplyPlan plan(List<RegisteredResource> existingResources,
                                  List<ResourceBinding> baseBindings,
                                  ResourceSnapshot snapshot,
                                  PermissionRoleLookup roleLookup) {
        if (snapshot == null) {
            throw new BusinessException("注册计划生成失败：snapshot 不能为空");
        }
        List<ResourceBinding> base = baseBindings == null ? List.of() : baseBindings;
        Map<ResourceKey, RegisteredResource> existingByKey = index(existingResources, "目标 Release 已有资源");
        Map<ResourceKey, ResourceBinding> baseByKey = indexBindings(base, "Diff 基准");

        ResourceDiffResult diff = diffEngine.diff(base, snapshot, roleLookup);
        Set<ResourceKey> rebindKeys = new LinkedHashSet<>();
        for (ResourceDiffEntry entry : diff.rebinds()) {
            rebindKeys.add(entry.key());
        }

        List<ResourceMutation> mutations = new ArrayList<>();
        collectSnapshotMutations(snapshot, existingByKey, baseByKey, rebindKeys, mutations);
        collectDeprecations(diff, existingByKey, mutations);

        mutations.sort(MUTATION_ORDER);
        return new RegistryApplyPlan(diff, mutations, collectTouchedPermissions(mutations));
    }

    /**
     * 按快照物化资源：该 Release 缺失的建行，已存在的只在需要变更时产出变更。
     */
    private void collectSnapshotMutations(ResourceSnapshot snapshot,
                                          Map<ResourceKey, RegisteredResource> existingByKey,
                                          Map<ResourceKey, ResourceBinding> baseByKey,
                                          Set<ResourceKey> rebindKeys,
                                          List<ResourceMutation> mutations) {
        for (ResourceBinding binding : ResourceDiffEngine.toBindings(snapshot)) {
            ResourceKey key = binding.key();
            RegisteredResource existing = existingByKey.get(key);
            boolean rebind = rebindKeys.contains(key);

            if (existing == null) {
                if (rebind) {
                    ResourceBinding baseBinding = baseByKey.get(key);
                    if (baseBinding == null) {
                        // Diff 只在基准存在该键时才报 REBIND，走到这里说明基准集合被外部改错。
                        throw new BusinessException("注册计划生成失败：换绑资源在 Diff 基准中缺失 " + key.asText());
                    }
                    // 4.9：新 Release 上的换绑同样不自动生效，先沿用基准（上一版本）的生效绑定。
                    mutations.add(ResourceMutation.rebind(key, binding.resourceId(),
                            baseBinding.permission(), binding.permission()));
                } else {
                    mutations.add(ResourceMutation.of(ResourceDiffType.ADD, key, binding.resourceId(),
                            binding.permission(), ResourceStatus.ACTIVE));
                }
                continue;
            }

            if (rebind) {
                // 换绑不自动生效（4.9）：生效绑定保持旧值，新值挂起等待管理员确认。
                mutations.add(ResourceMutation.rebind(key, binding.resourceId(),
                        existing.binding().permission(), binding.permission()));
                continue;
            }

            if (existing.status() != ResourceStatus.ACTIVE) {
                // 回滚重新被引用（4.11），或快照回退了换绑改动：挂起候选作废，资源恢复生效。
                // 生效绑定取已有值而不是快照值：绑定只能由管理员确认换绑来改变。
                mutations.add(ResourceMutation.of(ResourceDiffType.UPDATE, key, binding.resourceId(),
                        existing.binding().permission(), ResourceStatus.ACTIVE));
                continue;
            }

            if (isMetadataChanged(existing.binding(), binding)) {
                mutations.add(ResourceMutation.of(ResourceDiffType.UPDATE, key, binding.resourceId(),
                        existing.binding().permission(), ResourceStatus.ACTIVE));
            }
        }
    }

    /**
     * 废弃：只处理「目标 Release 自己曾经有、而新快照不再上报」的资源。
     *
     * <p>新 Release 首次注册时基准取上一个 Release，其中的 REMOVE 条目<b>不落库</b>——
     * 那些资源属于仍在服务的旧 Release（4.4/4.5），必须保持生效直到旧 Release 退役（4.8）。</p>
     */
    private void collectDeprecations(ResourceDiffResult diff,
                                     Map<ResourceKey, RegisteredResource> existingByKey,
                                     List<ResourceMutation> mutations) {
        for (ResourceDiffEntry removal : diff.removals()) {
            RegisteredResource existing = existingByKey.get(removal.key());
            if (existing == null) {
                continue;
            }
            mutations.add(ResourceMutation.of(ResourceDiffType.REMOVE, removal.key(),
                    existing.binding().resourceId(), existing.binding().permission(), ResourceStatus.DEPRECATED));
        }
    }

    /**
     * 汇总本次注册触及的权限码：生效绑定 + 换绑候选 + 被废弃资源的原有绑定。
     *
     * <p>匿名资源不产生权限点，因此排除 {@code ANONYMOUS} 字面量。</p>
     */
    private Set<String> collectTouchedPermissions(List<ResourceMutation> mutations) {
        Set<String> touched = new LinkedHashSet<>();
        for (ResourceMutation mutation : mutations) {
            addPermission(touched, mutation.permissionCode());
            addPermission(touched, mutation.pendingPermissionCode());
        }
        return touched;
    }

    private static void addPermission(Set<String> target, String permissionCode) {
        if (permissionCode == null || permissionCode.isBlank()) {
            return;
        }
        if (ResourceBinding.ANONYMOUS_PERMISSION.equals(permissionCode)) {
            return;
        }
        target.add(permissionCode);
    }

    /**
     * 元数据变化判定：{@code (method, path)} 与绑定都未变，仅 SDK 资源标识变化（4.1 的 UPDATE）。
     *
     * <p>与 {@code ResourceDiffEngine} 同一口径：任一侧缺少 {@code resourceId} 时不判定为变化，
     * 否则「旧数据没有该字段」会被误报成一次更新。</p>
     */
    private static boolean isMetadataChanged(ResourceBinding previous, ResourceBinding current) {
        if (previous.resourceId() == null || current.resourceId() == null) {
            return false;
        }
        return !previous.resourceId().equals(current.resourceId());
    }

    private static Map<ResourceKey, RegisteredResource> index(List<RegisteredResource> resources, String side) {
        Map<ResourceKey, RegisteredResource> indexed = new HashMap<>();
        if (resources == null) {
            return indexed;
        }
        for (RegisteredResource resource : resources) {
            if (resource == null) {
                throw new BusinessException("注册计划生成失败：" + side + "中存在 null 元素");
            }
            RegisteredResource duplicated = indexed.putIfAbsent(resource.key(), resource);
            if (duplicated != null) {
                throw new BusinessException("注册计划生成失败：" + side + "中存在重复资源 " + resource.key().asText());
            }
        }
        return indexed;
    }

    private static Map<ResourceKey, ResourceBinding> indexBindings(List<ResourceBinding> bindings, String side) {
        Map<ResourceKey, ResourceBinding> indexed = new HashMap<>();
        for (ResourceBinding binding : bindings) {
            if (binding == null) {
                throw new BusinessException("注册计划生成失败：" + side + "中存在 null 元素");
            }
            ResourceBinding duplicated = indexed.putIfAbsent(binding.key(), binding);
            if (duplicated != null) {
                throw new BusinessException("注册计划生成失败：" + side + "中存在重复资源 " + binding.key().asText());
            }
        }
        return indexed;
    }
}
