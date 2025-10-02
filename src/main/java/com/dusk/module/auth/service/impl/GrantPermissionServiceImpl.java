package com.dusk.module.auth.service.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import com.dusk.common.core.annotation.DisableTenantFilter;
import com.dusk.common.core.auth.permission.RoleInfo;
import com.dusk.common.core.service.impl.BaseService;
import com.dusk.module.auth.common.permission.IAuthPermissionManager;
import com.dusk.module.auth.entity.GrantPermission;
import com.dusk.module.auth.entity.QGrantPermission;
import com.dusk.module.auth.entity.QRole;
import com.dusk.module.auth.entity.Role;
import com.dusk.module.auth.repository.IGrantPermissionRepository;
import com.dusk.module.auth.service.IGrantPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author kefuming
 * @date 2021-08-27 14:02
 */
@Service
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class GrantPermissionServiceImpl extends BaseService<GrantPermission, IGrantPermissionRepository> implements IGrantPermissionService {

    @Autowired
    JPAQueryFactory queryFactory;
    @Autowired
    IAuthPermissionManager permissionManager;


    @Override
    @DisableTenantFilter
    public Map<String, List<RoleInfo>> getAll() {
        QGrantPermission grantPermission = QGrantPermission.grantPermission;
        Map<String, List<RoleInfo>> result = queryFactory.selectDistinct(grantPermission.tenantId, grantPermission.name, grantPermission.role.id, grantPermission.role.roleCode, grantPermission.role.roleName).from(grantPermission).leftJoin(grantPermission.role).fetch()
                .stream().collect(Collectors.groupingBy(p -> (p.get(grantPermission.tenantId) == null ? "" : p.get(grantPermission.tenantId)) + p.get(grantPermission.name), Collectors.mapping(p -> {
                    RoleInfo roleInfo = new RoleInfo();
                    roleInfo.setId(p.get(grantPermission.role.id).toString());
                    roleInfo.setRoleName(p.get(grantPermission.role.roleName));
                    roleInfo.setRoleCode(p.get(grantPermission.role.roleCode));
                    return roleInfo;
                }, Collectors.toList())));
        return result;
    }

    @Override
    public void addDynamicPermission(List<String> name, List<Long> roleIds, String businessKey) {
        List<Role> fetch = queryFactory.selectFrom(QRole.role).where(QRole.role.id.in(roleIds)).fetch();
        List<GrantPermission> addPermissions = new ArrayList<>();
        name.forEach(q -> {
            fetch.forEach(p -> {
                GrantPermission permission = new GrantPermission();
                permission.setBusinessKey(businessKey);
                permission.setRole(p);
                permission.setName(q);
                addPermissions.add(permission);
            });
        });
        if (addPermissions.size() > 0) {
            repository.saveAll(addPermissions);
            permissionManager.refreshAll();
        }
    }

    @Override
    public void deleteDynamicPermission(String businessKey) {
        long execute = queryFactory.delete(QGrantPermission.grantPermission).where(QGrantPermission.grantPermission.businessKey.eq(businessKey)).execute();
        if (execute > 0) {
            permissionManager.refreshAll();
        }
    }
}
