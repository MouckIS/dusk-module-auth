package com.dusk.module.auth.authz.readmodel;

import com.dusk.common.core.auth.registry.AuthzScope;
import com.dusk.common.core.enums.UserStatus;
import com.dusk.common.rpc.authz.RolePermissionSnapshot;
import com.dusk.common.rpc.authz.SubjectSnapshot;
import com.dusk.module.auth.authz.version.IAuthzRoleVersionRepository;
import com.dusk.module.auth.authz.version.IAuthzSubjectVersionRepository;
import com.dusk.module.auth.entity.GrantPermission;
import com.dusk.module.auth.entity.Role;
import com.dusk.module.auth.entity.User;
import com.dusk.module.auth.registry.entity.PermissionDefinition;
import com.dusk.module.auth.registry.enums.PermissionStatus;
import com.dusk.module.auth.registry.repository.IPermissionDefinitionRepository;
import com.dusk.module.auth.repository.IRoleRepository;
import com.dusk.module.auth.repository.IUserRepository;
import com.dusk.module.auth.service.ITenantPermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 角色权限与账号状态的唯一解析入口，见《权限优化方案-整理版》5.2 / 5.3。
 *
 * <p>Redis 投影与 Dubbo 回源共用本类，确保「网关照缓存判定」与「回源判定」看到同一份数据：</p>
 * <ul>
 *   <li>角色权限：RolePermission 事实 ∩ TenantPermission → 再过滤掉 ORPHANED/DISABLED；</li>
 *   <li>账号状态：{@code active} + {@code userStatus != Dimission}，并返回 {@code subjectVersion}。</li>
 * </ul>
 */
@Service
@Slf4j
public class AuthorizationReadModelResolver {

    private final IRoleRepository roleRepository;
    private final IUserRepository userRepository;
    private final IPermissionDefinitionRepository permissionRepository;
    private final ITenantPermissionService tenantPermissionService;
    private final IAuthzRoleVersionRepository roleVersionRepository;
    private final IAuthzSubjectVersionRepository subjectVersionRepository;

    public AuthorizationReadModelResolver(IRoleRepository roleRepository,
                                          IUserRepository userRepository,
                                          IPermissionDefinitionRepository permissionRepository,
                                          ITenantPermissionService tenantPermissionService,
                                          IAuthzRoleVersionRepository roleVersionRepository,
                                          IAuthzSubjectVersionRepository subjectVersionRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.permissionRepository = permissionRepository;
        this.tenantPermissionService = tenantPermissionService;
        this.roleVersionRepository = roleVersionRepository;
        this.subjectVersionRepository = subjectVersionRepository;
    }

    /**
     * 账号是否可用：{@code active} 为 true，且状态不是离职。
     */
    public static boolean enabled(User user) {
        if (!user.isActive()) {
            return false;
        }
        return user.getUserStatus() == null || user.getUserStatus() == UserStatus.OnJob;
    }

    /**
     * 解析角色在当前作用域下的有效权限。
     *
     * @return 角色不存在、作用域不匹配时返回 {@link Optional#empty()}
     */
    @Transactional(readOnly = true)
    public Optional<RolePermissionSnapshot> resolveRolePermissions(String tenantScope, Long roleId) {
        String scope = AuthzScope.require(tenantScope);
        if (roleId == null) {
            return Optional.empty();
        }
        Optional<Role> role = roleRepository.findById(roleId.longValue());
        if (role.isEmpty()) {
            // 角色已被删除：只要存在版本记录，就返回“空权限”快照，让投影覆盖旧缓存，
            // 而不是让已删除角色的旧权限一直存活到 TTL 到期。
            return roleVersionRepository.findByTenantScopeAndRoleId(scope, roleId)
                    .map(row -> new RolePermissionSnapshot(scope, roleId, row.getPermissionVersion(), Set.of()));
        }
        Long scopeTenantId = AuthzScope.tenantId(scope);
        if (!matches(role.get().getTenantId(), scopeTenantId)) {
            log.debug("角色作用域不匹配，拒绝回源：scope={}, roleId={}, roleTenantId={}",
                    scope, roleId, role.get().getTenantId());
            return Optional.empty();
        }

        Set<String> granted = role.get().getPermissions() == null ? Set.of()
                : role.get().getPermissions().stream()
                .map(GrantPermission::getName)
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<String> allowedByTenant = tenantAllowed(scope, scopeTenantId, granted);
        Set<String> effective = filterRuntimePermissions(allowedByTenant);
        long version = roleVersionRepository.findByTenantScopeAndRoleId(scope, roleId)
                .map(versionRow -> versionRow.getPermissionVersion())
                .orElse(0L);
        return Optional.of(new RolePermissionSnapshot(scope, roleId, version, effective));
    }

    /**
     * 解析账号状态与用户代际。
     */
    @Transactional(readOnly = true)
    public Optional<SubjectSnapshot> resolveSubject(String tenantScope, Long userId) {
        String scope = AuthzScope.require(tenantScope);
        if (userId == null) {
            return Optional.empty();
        }
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            // 用户已被删除：只要存在代际记录，就返回 enabled=false 的快照，
            // 让投影/回源显式失效旧令牌，而不是因为「查不到」把账号状态判成未知。
            return subjectVersionRepository.findByTenantScopeAndUserId(scope, userId)
                    .map(row -> new SubjectSnapshot(scope, userId, row.getSubjectVersion(), false, Set.of()));
        }
        Long scopeTenantId = AuthzScope.tenantId(scope);
        if (!matches(user.get().getTenantId(), scopeTenantId)) {
            log.debug("用户作用域不匹配，拒绝回源：scope={}, userId={}, userTenantId={}",
                    scope, userId, user.get().getTenantId());
            return Optional.empty();
        }
        Set<Long> roleIds = user.get().getUserRoles() == null ? Set.of()
                : user.get().getUserRoles().stream().map(Role::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        long version = subjectVersionRepository.findByTenantScopeAndUserId(scope, userId)
                .map(row -> row.getSubjectVersion())
                .orElse(0L);
        return Optional.of(new SubjectSnapshot(scope, userId, version, enabled(user.get()), roleIds));
    }

    private boolean matches(Long entityTenantId, Long scopeTenantId) {
        return scopeTenantId == null ? entityTenantId == null : scopeTenantId.equals(entityTenantId);
    }

    private Set<String> tenantAllowed(String scope, Long tenantId, Set<String> granted) {
        if (AuthzScope.isHost(scope)) {
            return granted;
        }
        try {
            Set<String> allowed = new HashSet<>(tenantPermissionService.getGrantedPermissionByTenantId(tenantId));
            granted.retainAll(allowed);
            return granted;
        } catch (Exception e) {
            // 租户不存在/版本缺失时按默认拒绝处理：宁可 403，也不能把未授权权限放出去。
            log.warn("租户权限解析失败，按空集合处理：scope={}, tenantId={}", scope, tenantId, e);
            return Set.of();
        }
    }

    /**
     * 只保留运行时有效的权限：ORPHANED / DISABLED 不进入读模型（3.3）。
     *
     * <p>注册表还没有该权限码的记录时保留（迁移期兼容）：权限码来自既有 RolePermission，
     * 若因为尚未回填注册表就把它删掉，会让存量角色在升级窗口内全部 403。</p>
     */
    private Set<String> filterRuntimePermissions(Set<String> codes) {
        if (codes.isEmpty()) {
            return Set.of();
        }
        Map<String, PermissionStatus> statusByCode = permissionRepository.findByCodeIn(codes).stream()
                .collect(Collectors.toMap(PermissionDefinition::getCode, PermissionDefinition::getStatus,
                        (left, right) -> left));
        return codes.stream()
                .filter(code -> {
                    PermissionStatus status = statusByCode.get(code);
                    return status == null || status == PermissionStatus.ACTIVE;
                })
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
