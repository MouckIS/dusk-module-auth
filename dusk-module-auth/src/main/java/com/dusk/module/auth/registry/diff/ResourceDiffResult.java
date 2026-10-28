package com.dusk.module.auth.registry.diff;

import com.dusk.module.auth.registry.enums.ResourceDiffType;

import java.util.List;

/**
 * Diff 结果集合，见《权限优化方案-整理版》4.1。
 *
 * <p>结果顺序稳定（按类型、再按 {@code method + path} 排序），
 * 便于日志比对、告警去重与测试断言。</p>
 */
public record ResourceDiffResult(List<ResourceDiffEntry> entries) {

    public ResourceDiffResult {
        entries = entries == null ? List.of() : List.copyOf(entries);
    }

    public static ResourceDiffResult empty() {
        return new ResourceDiffResult(List.of());
    }

    /**
     * 是否没有任何变化。为 {@code true} 时调用方应直接幂等跳过写库。
     */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public int totalChanges() {
        return entries.size();
    }

    /**
     * 按类型筛选。
     */
    public List<ResourceDiffEntry> ofType(ResourceDiffType type) {
        return entries.stream().filter(entry -> entry.type() == type).toList();
    }

    public List<ResourceDiffEntry> additions() {
        return ofType(ResourceDiffType.ADD);
    }

    public List<ResourceDiffEntry> removals() {
        return ofType(ResourceDiffType.REMOVE);
    }

    public List<ResourceDiffEntry> updates() {
        return ofType(ResourceDiffType.UPDATE);
    }

    public List<ResourceDiffEntry> rebinds() {
        return ofType(ResourceDiffType.REBIND);
    }

    /**
     * 是否存在换绑：存在即必须挂起等待管理员确认，不能随发布自动生效（4.9）。
     */
    public boolean hasRebind() {
        return entries.stream().anyMatch(entry -> entry.type() == ResourceDiffType.REBIND);
    }
}
