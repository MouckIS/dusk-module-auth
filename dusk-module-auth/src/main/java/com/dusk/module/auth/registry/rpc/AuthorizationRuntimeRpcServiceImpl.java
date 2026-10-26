package com.dusk.module.auth.registry.rpc;

import com.dusk.common.core.auth.registry.AuthzScope;
import com.dusk.common.rpc.authz.IAuthorizationRuntimeRpcService;
import com.dusk.common.rpc.authz.ResourceRuntimeSnapshot;
import com.dusk.common.rpc.authz.RolePermissionSnapshot;
import com.dusk.common.rpc.authz.SubjectSnapshot;
import com.dusk.module.auth.authz.readmodel.AuthorizationReadModelResolver;
import com.dusk.module.auth.registry.service.ResourceRuntimeResolver;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.Optional;

/**
 * 运行态回源 Dubbo Provider，见《权限优化方案-整理版》5.2 / 8.2。
 *
 * <p>回源只在网关 L1/L2 都未命中时发生；本实现直接读取 PostgreSQL（唯一事实源），
 * 不把 Redis 当作事实来源。返回结果都带版本号，供网关写入缓存前再次比较。</p>
 */
@DubboService
@Slf4j
public class AuthorizationRuntimeRpcServiceImpl implements IAuthorizationRuntimeRpcService {

    private final ResourceRuntimeResolver resourceRuntimeResolver;
    private final AuthorizationReadModelResolver authorizationReadModelResolver;

    public AuthorizationRuntimeRpcServiceImpl(ResourceRuntimeResolver resourceRuntimeResolver,
                                              AuthorizationReadModelResolver authorizationReadModelResolver) {
        this.resourceRuntimeResolver = resourceRuntimeResolver;
        this.authorizationReadModelResolver = authorizationReadModelResolver;
    }

    @Override
    public ResourceRuntimeSnapshot getResourceSnapshot(String serviceId) {
        Optional<ResourceRuntimeSnapshot> snapshot = resourceRuntimeResolver.resolve(serviceId);
        if (snapshot.isEmpty()) {
            log.debug("运行态回源：服务无有效资源并集，serviceId={}", serviceId);
            return null;
        }
        return snapshot.get();
    }

    @Override
    public RolePermissionSnapshot getRolePermissions(String tenantScope, Long roleId) {
        try {
            return authorizationReadModelResolver.resolveRolePermissions(AuthzScope.require(tenantScope), roleId)
                    .orElse(null);
        } catch (Exception e) {
            log.warn("运行态回源角色权限失败：scope={}, roleId={}", tenantScope, roleId, e);
            return null;
        }
    }

    @Override
    public SubjectSnapshot getSubject(String tenantScope, Long userId) {
        try {
            return authorizationReadModelResolver.resolveSubject(AuthzScope.require(tenantScope), userId)
                    .orElse(null);
        } catch (Exception e) {
            log.warn("运行态回源账号状态失败：scope={}, userId={}", tenantScope, userId, e);
            return null;
        }
    }
}
