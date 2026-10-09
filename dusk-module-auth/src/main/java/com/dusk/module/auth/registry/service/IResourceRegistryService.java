package com.dusk.module.auth.registry.service;

import com.dusk.common.core.auth.registry.ResourceSnapshot;
import com.dusk.module.auth.registry.dto.RegistryRegistrationResponse;

/**
 * 资源快照注册，见《权限优化方案-整理版》3.4。
 *
 * <p>这是 3.5「方案 A：同步注册 + Readiness 门控」在 Auth 侧的落点：SDK 在启动期把
 * 当前完整 API 集合（红线③：不是增量）同步提交到 {@code POST /internal/registry/snapshot}，
 * Auth 在一个事务内完成 Diff 与落库，返回确认后 SDK 的 readiness 才就绪。</p>
 */
public interface IResourceRegistryService {

    /**
     * 接纳一份完整快照并落库。
     *
     * <p>幂等：同一 {@code (serviceId, serviceVersion)} 重复上报且 {@code resourceVersion} 未变时直接返回，
     * 不做任何写库（3.4 第 4 步）。</p>
     *
     * @param snapshot SDK 上报的完整快照，构造期已完成字段级校验
     * @return 本次注册的结果与变更清单
     */
    RegistryRegistrationResponse register(ResourceSnapshot snapshot);
}
