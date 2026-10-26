package com.dusk.module.auth.registry.rpc;

import com.dusk.common.core.auth.registry.ResourceSnapshot;
import com.dusk.common.rpc.authz.IPermissionRegistryRpcService;
import com.dusk.common.rpc.authz.ResourceRegistrationResult;
import com.dusk.module.auth.registry.service.IResourceRegistryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * 权限注册 Dubbo Provider，见《权限优化方案-整理版》4.3。
 *
 * <p>调用方身份由 Dubbo Provider 过滤器/服务级 ACL 约束；本实现只处理业务结果，
 * 不把异常透传给消费者——确定性失败必须映射为稳定结果码，让 SDK 的 readiness 逻辑
 * 不依赖异常类型。</p>
 */
@DubboService
@Slf4j
public class PermissionRegistryRpcServiceImpl implements IPermissionRegistryRpcService {

    private final IResourceRegistryService resourceRegistryService;

    public PermissionRegistryRpcServiceImpl(IResourceRegistryService resourceRegistryService) {
        this.resourceRegistryService = resourceRegistryService;
    }

    @Override
    public ResourceRegistrationResult registerSnapshot(ResourceSnapshot snapshot) {
        if (snapshot == null) {
            return ResourceRegistrationResult.invalid("snapshot 不能为空");
        }
        long begin = System.nanoTime();
        ResourceRegistrationResult result = resourceRegistryService.registerSnapshot(snapshot);
        log.info("Dubbo 快照注册：serviceId={}, serviceVersion={}, resourceVersion={}, result={}, revision={}, releaseId={}, 耗时={}ms",
                snapshot.serviceId(), snapshot.serviceVersion(), snapshot.resourceVersion(),
                result.resultCode(), result.resourceRevision(), result.releaseId(),
                (System.nanoTime() - begin) / 1_000_000);
        return result;
    }
}
