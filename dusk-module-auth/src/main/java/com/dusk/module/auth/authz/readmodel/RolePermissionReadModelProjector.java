package com.dusk.module.auth.authz.readmodel;

import com.dusk.common.rpc.authz.RolePermissionSnapshot;
import com.dusk.module.auth.authz.config.AuthzMqProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

/**
 * 角色权限 Redis 投影：{@code auth:authorization:{scope}:role:{roleId}}，见 5.2 / 8.2。
 *
 * <p>只从 PG 重算；写入按 {@code permissionVersion} 单调覆盖。投影成功后由 Outbox 发送
 * {@code PERMISSION_CHANGED}，因此网关收到失效事件时 L2 必已是新值。</p>
 */
@Component
@Slf4j
public class RolePermissionReadModelProjector {

    public static final String ROLE_KEY_PREFIX = "auth:authorization:";
    public static final String PERMISSION_VERSION_FIELD = "permissionVersion";

    private final AuthorizationReadModelResolver resolver;
    private final RedisVersionedWriter writer;
    private final AuthzMqProperties properties;
    private final ObjectMapper objectMapper;

    public RolePermissionReadModelProjector(AuthorizationReadModelResolver resolver,
                                            RedisVersionedWriter writer,
                                            AuthzMqProperties properties,
                                            ObjectMapper objectMapper) {
        this.resolver = resolver;
        this.writer = writer;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * 重建某角色的权限读模型。
     *
     * @return 投影是否成功（角色不存在视为失败，避免发送无内容可读的变更事件）
     */
    public boolean project(String tenantScope, Long roleId) {
        if (roleId == null || tenantScope == null || tenantScope.isBlank()) {
            return false;
        }
        try {
            Optional<RolePermissionSnapshot> snapshot = resolver.resolveRolePermissions(tenantScope, roleId);
            if (snapshot.isEmpty()) {
                log.warn("角色权限读模型跳过：角色不存在或作用域不匹配，scope={}, roleId={}", tenantScope, roleId);
                return false;
            }
            RolePermissionSnapshot role = snapshot.get();
            String payload = objectMapper.writeValueAsString(new RolePermissionPayload(
                    role.tenantScope(), role.roleId(), role.permissionVersion(), List.copyOf(role.permissions())));
            return writer.write(key(role.tenantScope(), role.roleId()), PERMISSION_VERSION_FIELD,
                    role.permissionVersion(), payload, properties.getReadModelTtl());
        } catch (Exception e) {
            log.error("角色权限读模型投影失败：scope={}, roleId={}", tenantScope, roleId, e);
            return false;
        }
    }

    private String key(String scope, Long roleId) {
        return ROLE_KEY_PREFIX + scope + ":role:" + roleId;
    }

    /**
     * JSON 结构必须与网关 {@code AuthorizationReadModelJson.parseRoleGrant} 一致。
     */
    public record RolePermissionPayload(String tenantScope,
                                        Long roleId,
                                        long permissionVersion,
                                        List<String> permissions) {
    }
}
