package com.dusk.module.auth.registry.diff;

import com.dusk.module.auth.registry.enums.ResourceDiffType;

/**
 * 一条 Diff 结果，见《权限优化方案-整理版》4.1 / 4.9。
 *
 * @param type         结果类型
 * @param key          资源定位键
 * @param previous     变更前的绑定；{@link ResourceDiffType#ADD} 时为 {@code null}
 * @param current      变更后的绑定；{@link ResourceDiffType#REMOVE} 时为 {@code null}
 * @param rebindImpact 换绑影响面；仅 {@link ResourceDiffType#REBIND} 时非 {@code null}
 */
public record ResourceDiffEntry(
        ResourceDiffType type,
        ResourceKey key,
        ResourceBinding previous,
        ResourceBinding current,
        RebindImpact rebindImpact) {

    /**
     * 便捷构造：非换绑场景。
     */
    public static ResourceDiffEntry of(ResourceDiffType type, ResourceBinding previous, ResourceBinding current) {
        ResourceKey key = current != null ? current.key() : previous.key();
        return new ResourceDiffEntry(type, key, previous, current, null);
    }

    /**
     * 换绑场景。
     */
    public static ResourceDiffEntry rebind(ResourceBinding previous, ResourceBinding current, RebindImpact impact) {
        return new ResourceDiffEntry(ResourceDiffType.REBIND, previous.key(), previous, current, impact);
    }

    /**
     * 换绑前的权限码；ADD 时为 {@code null}。
     */
    public String previousPermission() {
        return previous == null ? null : previous.permission();
    }

    /**
     * 换绑后的权限码；REMOVE 时为 {@code null}。
     */
    public String currentPermission() {
        return current == null ? null : current.permission();
    }

    @Override
    public String toString() {
        return switch (type) {
            case ADD -> "ADD " + key.asText() + " -> " + currentPermission();
            case REMOVE -> "REMOVE " + key.asText() + " (原 " + previousPermission() + ")";
            case UPDATE -> "UPDATE " + key.asText() + " (" + currentPermission() + ")";
            case REBIND -> "REBIND " + key.asText() + " : " + previousPermission() + " -> " + currentPermission();
        };
    }
}
