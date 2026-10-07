package com.dusk.module.auth.registry.diff;

import com.dusk.common.core.auth.registry.ResourceSnapshot;
import com.dusk.common.core.auth.registry.SnapshotAnonymousResource;
import com.dusk.common.core.auth.registry.SnapshotResource;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.module.auth.registry.enums.ResourceDiffType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Resource Diff 引擎，见《权限优化方案-整理版》4.1。
 *
 * <p>Auth 侧 Diff 由本引擎承担（3.4 事务内第 5 步）：不做增量比对，而是拿
 * 「完整 Snapshot」与「既有绑定集合」求差，因此天然幂等——重复上报同一 Snapshot 得到空结果。</p>
 *
 * <p>结果是纯数据，不落库、不发事件；状态迁移与持久化由上层编排（本批范围外）。
 * 这样保证 Diff 逻辑可以脱离 Spring 上下文做穷尽单测。</p>
 *
 * <p><b>基准集合的口径</b>：同一 {@code (serviceId, serviceVersion)} 重复注册时，
 * 基准取该 Release 已有资源（幂等）；新 Release 首次注册时，基准取上一 Release 的资源，
 * 用于产出 REMOVE 候选与冲突告警（见 4.5 与 5.7：旧 Release 独有的资源随并集保留）。</p>
 */
public class ResourceDiffEngine {

    /**
     * 结果顺序：先按类型分组，再按 method、path 排序，保证日志与告警稳定可 diff。
     */
    private static final Comparator<ResourceDiffEntry> ENTRY_ORDER =
            Comparator.comparing((ResourceDiffEntry entry) -> entry.type().ordinal())
                    .thenComparing(entry -> entry.key().method())
                    .thenComparing(entry -> entry.key().path());

    /**
     * 与既有绑定集合求差。
     *
     * @param previous 基准绑定集合（可为空集合，表示该 Release 首次注册）
     * @param snapshot SDK 上报的完整快照
     */
    public ResourceDiffResult diff(List<ResourceBinding> previous, ResourceSnapshot snapshot) {
        return diff(previous, snapshot, null);
    }

    /**
     * 与既有绑定集合求差，并解析换绑影响面。
     *
     * @param roleLookup 角色查询；为 {@code null} 时换绑仍会被识别，但影响面为空清单
     */
    public ResourceDiffResult diff(List<ResourceBinding> previous, ResourceSnapshot snapshot, PermissionRoleLookup roleLookup) {
        if (snapshot == null) {
            throw new BusinessException("Resource Diff 失败：snapshot 不能为空");
        }
        return diff(previous, toBindings(snapshot), roleLookup);
    }

    /**
     * 与既有绑定集合求差（显式传入当前绑定集合，便于单测与跨 Release 比对）。
     */
    public ResourceDiffResult diff(List<ResourceBinding> previous,
                                   List<ResourceBinding> current,
                                   PermissionRoleLookup roleLookup) {
        Map<ResourceKey, ResourceBinding> previousByKey = index(previous, "基准");
        Map<ResourceKey, ResourceBinding> currentByKey = index(current, "当前");

        List<ResourceDiffEntry> entries = new ArrayList<>();

        for (Map.Entry<ResourceKey, ResourceBinding> item : currentByKey.entrySet()) {
            ResourceKey key = item.getKey();
            ResourceBinding now = item.getValue();
            ResourceBinding old = previousByKey.get(key);

            if (old == null) {
                entries.add(ResourceDiffEntry.of(ResourceDiffType.ADD, null, now));
                continue;
            }
            if (!old.permission().equals(now.permission())) {
                entries.add(ResourceDiffEntry.rebind(old, now, resolveImpact(old.permission(), now.permission(), roleLookup)));
                continue;
            }
            if (isMetadataChanged(old, now)) {
                entries.add(ResourceDiffEntry.of(ResourceDiffType.UPDATE, old, now));
            }
        }

        for (Map.Entry<ResourceKey, ResourceBinding> item : previousByKey.entrySet()) {
            if (!currentByKey.containsKey(item.getKey())) {
                entries.add(ResourceDiffEntry.of(ResourceDiffType.REMOVE, item.getValue(), null));
            }
        }

        entries.sort(ENTRY_ORDER);
        return new ResourceDiffResult(entries);
    }

    /**
     * 把 Snapshot 展开为参与 Diff 的绑定集合：受保护资源按自身权限码，匿名资源按
     * {@link ResourceBinding#ANONYMOUS_PERMISSION}。
     */
    public static List<ResourceBinding> toBindings(ResourceSnapshot snapshot) {
        List<ResourceBinding> bindings = new ArrayList<>(snapshot.totalResources());
        for (SnapshotResource resource : snapshot.resources()) {
            bindings.add(new ResourceBinding(resource.method(), resource.path(), resource.permission(), resource.resourceId()));
        }
        for (SnapshotAnonymousResource anonymous : snapshot.anonymousResources()) {
            bindings.add(ResourceBinding.anonymous(anonymous.method(), anonymous.path()));
        }
        return List.copyOf(bindings);
    }

    private static Map<ResourceKey, ResourceBinding> index(List<ResourceBinding> bindings, String side) {
        Map<ResourceKey, ResourceBinding> indexed = new HashMap<>();
        if (bindings == null) {
            return indexed;
        }
        for (ResourceBinding binding : bindings) {
            if (binding == null) {
                throw new BusinessException("Resource Diff 失败：" + side + "绑定集合中存在 null 元素");
            }
            ResourceBinding duplicated = indexed.putIfAbsent(binding.key(), binding);
            if (duplicated != null) {
                throw new BusinessException(
                        "Resource Diff 失败：" + side + "绑定集合中资源重复 " + binding.key().asText()
                                + "（5.10 要求路径精确唯一）");
            }
        }
        return indexed;
    }

    /**
     * 元数据变化判定：{@code (method, path)} 与 permission 都未变，
     * 但 SDK 上报的 {@code resourceId} 发生变化（4.1 的 UPDATE）。
     *
     * <p>任一侧缺少 {@code resourceId} 时不判定为变化——否则「旧数据没有该字段」
     * 会被误报为一次元数据更新。</p>
     */
    private static boolean isMetadataChanged(ResourceBinding previous, ResourceBinding current) {
        if (previous.resourceId() == null || current.resourceId() == null) {
            return false;
        }
        return !previous.resourceId().equals(current.resourceId());
    }

    /**
     * 解析换绑影响面（4.9）：失去访问权 = 持有旧权限但不持有新权限的角色；
     * 获得访问权 = 持有新权限但不持有旧权限的角色；两者皆持有则不受影响，不计入清单。
     */
    private static RebindImpact resolveImpact(String previousPermission,
                                              String newPermission,
                                              PermissionRoleLookup roleLookup) {
        if (roleLookup == null) {
            return RebindImpact.unknown(previousPermission, newPermission);
        }
        Set<Long> losing = safeRoleIds(roleLookup, previousPermission);
        Set<Long> gaining = safeRoleIds(roleLookup, newPermission);

        List<Long> losingOnly = losing.stream()
                .filter(roleId -> !gaining.contains(roleId))
                .sorted()
                .toList();
        List<Long> gainingOnly = gaining.stream()
                .filter(roleId -> !losing.contains(roleId))
                .sorted()
                .toList();

        return new RebindImpact(previousPermission, newPermission, losingOnly, gainingOnly);
    }

    /**
     * 角色查询容错：实现返回 {@code null} 或含 {@code null} 元素时按空集合处理，
     * 避免「影响面解析失败」阻断 Diff 主流程（Diff 的结论本身仍然可靠）。
     */
    private static Set<Long> safeRoleIds(PermissionRoleLookup roleLookup, String permissionCode) {
        Set<Long> roleIds = roleLookup.roleIdsHolding(permissionCode);
        if (roleIds == null || roleIds.isEmpty()) {
            return Set.of();
        }
        Set<Long> sanitized = new HashSet<>(roleIds.size());
        roleIds.stream().filter(Objects::nonNull).forEach(sanitized::add);
        return sanitized;
    }
}
