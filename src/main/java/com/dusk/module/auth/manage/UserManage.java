package com.dusk.module.auth.manage;

import com.dusk.module.auth.entity.GrantPermission;
import com.dusk.module.auth.entity.Role;
import com.dusk.module.auth.entity.User;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.dusk.common.framework.entity.BaseEntity;
import com.dusk.common.framework.model.UserContext;
import com.dusk.common.framework.tenant.TenantContextHolder;
import com.dusk.common.framework.utils.SecurityUtils;
import com.dusk.module.auth.common.permission.IAuthPermissionManager;
import com.dusk.module.auth.entity.*;
import com.dusk.module.auth.repository.IGrantPermissionRepository;
import com.dusk.module.auth.repository.IRoleRepository;
import com.dusk.module.auth.repository.IUserRepository;
import com.dusk.module.auth.service.ITenantPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author kefuming
 * @date 2020-08-05 15:50
 */
@Component
@Transactional
public class UserManage implements IUserManage {
    @Autowired
    IUserRepository userRepository;
    @Autowired
    IRoleRepository roleRepository;
    @Autowired
    SecurityUtils securityUtils;
    @Autowired
    IAuthPermissionManager permissionManager;
    @Autowired
    IGrantPermissionRepository grantPermissionRepository;
    @Autowired
    ITenantPermissionService tenantPermissionService;
    @Autowired
    JPAQueryFactory jpaQueryFactory;

    @Override
    public List<Role> getUserRole(Long userId) {
        Optional<User> data = userRepository.findById(userId);
        return data.map(User::getUserRoles).orElse(new ArrayList<>());
    }

    @Override
    public User getCurrentUser() {
        UserContext userContext = securityUtils.getCurrentUser();
        return (userContext == null ||  userContext.getId() == null ) ? null : getUserInfo(userContext.getId());
    }

    @Override
    public User getUserInfo(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }

    @Override
    public List<String> getCurrentUserPermissions() {
        Long userId = securityUtils.getCurrentUser().getId();
        return getUserPermissions(userId);
    }

    @Override
    public List<String> getUserPermissions(Long userId) {
        User userInfo = getUserInfo(userId);
        List<String> permissions;
        if (userInfo.getTenantId() == null) {
            permissions = permissionManager.getDefinitionPermission(!StringUtils.isEmpty(TenantContextHolder.getTenantId()));
        } else {
            permissions = tenantPermissionService.getGrantedPermissionByTenantId(userInfo.getTenantId());
        }
        if (userInfo.isAdmin()) {
            return permissions;
        } else {
            List<Long> roleIds = userInfo.getUserRoles().stream().map(BaseEntity::getId).collect(Collectors.toList());
            if (roleIds.size() > 0) {
                List<GrantPermission> grantPermissionList = grantPermissionRepository.findDistinctByRoleIdIn(roleIds.toArray(new Long[0]));
                return grantPermissionList.stream().filter(p -> permissions.contains(p.getName())).map(GrantPermission::getName).collect(Collectors.toList());
            }
        }
        return new ArrayList<>();
    }

    @Override
    public List<Long> getUserIdsByRoleName(List<String> roleNames) {
        QRole q_role = QRole.role;
        QUser q_user = QUser.user;
        if (roleNames != null) {
            return jpaQueryFactory.selectDistinct(q_user.id).from(q_user).where(q_user.userRoles.any().id.in(jpaQueryFactory.select(q_role.id).distinct().from(q_role).where(
                    q_role.roleName.in(roleNames)
            ))).fetch();
        }
        return new ArrayList<>();
    }
}
