package com.dusk.module.ddm.module.auth.service.impl;

import com.github.dozermapper.core.Mapper;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import com.dusk.common.core.auth.permission.Permission;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.service.impl.BaseService;
import com.dusk.common.module.auth.dto.role.RolePermissionDto;
import com.dusk.module.auth.common.config.AppAuthConfig;
import com.dusk.module.auth.common.permission.IAuthPermissionManager;
import com.dusk.module.auth.dto.permission.EditionPermissionInputDto;
import com.dusk.module.auth.entity.QTenant;
import com.dusk.module.auth.entity.QTenantPermission;
import com.dusk.module.auth.entity.Tenant;
import com.dusk.module.auth.entity.TenantPermission;
import com.dusk.module.auth.repository.ITenantPermissionRepository;
import com.dusk.module.auth.repository.ITenantRepository;
import com.dusk.module.auth.service.ITenantPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author kefuming
 * @date 2020-12-11 9:15
 */
@Service
@Transactional
@Slf4j
public class TenantPermissionServiceImpl extends BaseService<TenantPermission, ITenantPermissionRepository> implements ITenantPermissionService {
    @Autowired
    IAuthPermissionManager permissionManager;
    @Autowired
    JPAQueryFactory jpaQueryFactory;
    @Autowired
    Mapper dozerMapper;
    @Autowired
    ITenantRepository tenantRepository;
    @Autowired
    AppAuthConfig appAuthConfig;

    @Override
    public List<RolePermissionDto> getEditionPermissions(Long editionId) {
        if (editionId == null) {
            throw new BusinessException("异常请求！");
        }
        List<String> hasPermissions = getGrantedPermissionByEditionId(editionId);
        return getPermissionDto(hasPermissions);
    }

    @Override
    public void setEditionPermissions(EditionPermissionInputDto input) {
        //先删除之前的
        QTenantPermission qTenantPermission = QTenantPermission.tenantPermission;
        jpaQueryFactory.delete(qTenantPermission).where(qTenantPermission.editionId.eq(input.getId())).execute();

        //重新批量插入
        if (input.getPermissions() != null) {
            List<TenantPermission> saveDatas = new ArrayList<>();
            input.getPermissions().forEach(p -> {
                TenantPermission tenantPermission = new TenantPermission();
                tenantPermission.setEditionId(input.getId());
                tenantPermission.setName(p);
                saveDatas.add(tenantPermission);
            });
            if (saveDatas.size() > 0) {
                repository.saveAll(saveDatas);
            }
        }

        //要刷新权限缓存
        permissionManager.refreshAll();
    }

    @Override
    public List<RolePermissionDto> getTenantPermissions(Long tenantId) {
        List<String> permissions;
        if (appAuthConfig.isDisableTenantAuthFilter()) {
            permissions = permissionManager.getDefinitionPermission(true);
        } else {
            permissions = getGrantedPermissionByTenantId(tenantId);
        }
        return getPermissionDto(permissions);
    }


    @Override
    public List<String> getGrantedPermissionByTenantId(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow(() -> new BusinessException("数据不存在"));

        if (tenant.getEditionId() != null) {
            return getGrantedPermissionByEditionId(tenant.getEditionId());
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public Map<String, List<Long>> getAllTenantPermission() {
        QTenant qTenant = QTenant.tenant;
        QTenantPermission qTenantPermission = QTenantPermission.tenantPermission;

        List<String> allPermission = permissionManager.getDefinitionPermission(true);
        Map<String, List<Long>> resultMap = new HashMap<>();
        if (appAuthConfig.isDisableTenantAuthFilter()) {
            List<Long> fetch = jpaQueryFactory.select(qTenant.id).from(qTenant).fetch();
            allPermission.forEach(p -> {
                resultMap.put(p, fetch);
            });
        } else {
            Map<String, List<Tuple>> editionMap = jpaQueryFactory.select(qTenant.id, qTenantPermission.name).from(qTenant).innerJoin(qTenantPermission)
                    .on(qTenant.edition.id.eq(qTenantPermission.editionId)).where(qTenantPermission.editionId.isNotNull()).fetch().stream().collect(Collectors.groupingBy(p -> p.get(qTenantPermission.name)));
            allPermission.forEach(p -> {
                List<Long> hasPermissionTenantIds = new ArrayList<>();
                List<Tuple> edition = editionMap.get(p);
                if (edition != null) {
                    hasPermissionTenantIds.addAll(edition.stream().map(o -> o.get(qTenant.id)).collect(Collectors.toList()));
                }
                resultMap.put(p, hasPermissionTenantIds);
            });
        }

        return resultMap;
    }

    //region private


    private List<TenantPermission> getPermissionByEditionId(Long editionId) {
        QTenantPermission qTenantPermission = QTenantPermission.tenantPermission;
        List<TenantPermission> fetch = jpaQueryFactory.selectFrom(qTenantPermission).where(qTenantPermission.editionId.eq(editionId)).fetch();
        return fetch;
    }


    /**
     * 根据版本id获取已授权的权限名称列表
     *
     * @param editionId
     * @return
     */
    private List<String> getGrantedPermissionByEditionId(Long editionId) {
        List<TenantPermission> permissions = getPermissionByEditionId(editionId);
        return permissions.stream().map(p -> p.getName()).collect(Collectors.toList());
    }


    private List<RolePermissionDto> getPermissionDto(List<String> hasPermissions) {
        List<Permission> permissions = permissionManager.getDefinitionPermissionTree(true);
        List<RolePermissionDto> result = new ArrayList<>();
        permissions.forEach(s -> {
            RolePermissionDto rolePermissionDto = dozerMapper.map(s, RolePermissionDto.class);
            if (hasPermissions.contains(s.getName())) {
                rolePermissionDto.setGranted(true);
            } else {
                rolePermissionDto.setGranted(false);
            }
            result.add(rolePermissionDto);
        });
        return result;
    }

    //endregion
}
