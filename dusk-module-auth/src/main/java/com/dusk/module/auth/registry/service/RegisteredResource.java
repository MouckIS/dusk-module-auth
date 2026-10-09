package com.dusk.module.auth.registry.service;

import com.dusk.module.auth.registry.diff.ResourceBinding;
import com.dusk.module.auth.registry.diff.ResourceKey;
import com.dusk.module.auth.registry.enums.ResourceStatus;

/**
 * 已落库的 Resource 在计划阶段的视图：绑定 + 状态。
 *
 * <p>为什么不直接用 {@link ResourceBinding}：Diff 只关心 {@code (method, path) → permission}，
 * 而落库计划必须知道这条资源当前是 {@code ACTIVE}、{@code PENDING_REBIND} 还是 {@code DEPRECATED}——
 * 4.9 的「换绑未确认」与 4.11 的「回滚恢复」都取决于它。把状态塞进 {@code ResourceBinding}
 * 会污染 Diff 的值语义，因此单独建一个输入类型。</p>
 *
 * @param binding 绑定
 * @param status  该资源当前状态；调用方应在 null 时按 {@link ResourceStatus#ACTIVE} 处理
 */
public record RegisteredResource(ResourceBinding binding, ResourceStatus status) {

    public RegisteredResource {
        if (binding == null) {
            throw new IllegalArgumentException("RegisteredResource.binding 不能为空");
        }
        if (status == null) {
            throw new IllegalArgumentException("RegisteredResource.status 不能为空，位置：" + binding.key().asText());
        }
    }

    public static RegisteredResource of(ResourceBinding binding, ResourceStatus status) {
        return new RegisteredResource(binding, status);
    }

    public ResourceKey key() {
        return binding.key();
    }

    /**
     * 该资源是否参与运行时授权（4.9：换绑未确认期间仍按旧绑定放行）。
     */
    public boolean effectiveForRuntime() {
        return status.effectiveForRuntime();
    }
}
