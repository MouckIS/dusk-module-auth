package com.dusk.module.ddm.module.auth.service.impl;


import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.dusk.module.auth.dto.role.*;
import com.dusk.module.auth.entity.GrantPermission;
import com.dusk.module.auth.entity.Role;
import com.dusk.module.auth.entity.User;
import com.github.dozermapper.core.Mapper;
import com.querydsl.core.types.QBean;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.Service;
import com.dusk.common.core.auth.permission.Permission;
import com.dusk.common.core.dto.EntityDto;
import com.dusk.common.core.dto.PagedAndSortedInputDto;
import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.common.core.entity.BaseEntity;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.jpa.Specifications;
import com.dusk.common.core.jpa.querydsl.QBeanBuilder;
import com.dusk.common.core.service.impl.BaseService;
import com.dusk.common.core.tenant.TenantContextHolder;
import com.dusk.common.core.utils.DozerUtils;
import com.dusk.common.core.utils.UtBeanUtils;
import com.dusk.common.module.auth.dto.RoleSimpleDto;
import com.dusk.common.module.auth.dto.orga.OrganizationUnitUserListDto;
import com.dusk.common.module.auth.dto.role.RoleListDto;
import com.dusk.common.module.auth.dto.role.RolePermissionDto;
import com.dusk.common.module.auth.service.IRoleRpcService;
import com.dusk.module.auth.common.permission.IAuthPermissionManager;
import com.dusk.module.auth.dto.orga.BindRoleToOrgInput;
import com.dusk.module.auth.dto.role.*;
import com.dusk.module.auth.dto.user.BindRoleToUserInput;
import com.dusk.module.auth.dto.user.GetUserByRoleDto;
import com.dusk.module.auth.dto.user.UnbindRoleForUserDto;
import com.dusk.module.auth.entity.*;
import com.dusk.module.auth.excel.RolePermissionExportRowWriteHandler;
import com.dusk.module.auth.repository.IGrantPermissionRepository;
import com.dusk.module.auth.repository.IRoleRepository;
import com.dusk.module.auth.repository.IUserRepository;
import com.dusk.module.auth.service.IOrganizationUnitService;
import com.dusk.module.auth.service.IRoleService;
import com.dusk.module.auth.service.ITenantPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class RoleServiceImpl extends BaseService<Role, IRoleRepository> implements IRoleRpcService, IRoleService {

    @Autowired
    private IRoleRepository repository;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private IOrganizationUnitService organizationUnitService;

    @Autowired
    private IGrantPermissionRepository permissionRepository;

    @Autowired
    private Mapper dozerMapper;

    @Autowired
    IAuthPermissionManager permissionManager;

    @Autowired
    ITenantPermissionService tenantPermissionService;

    @Autowired
    JPAQueryFactory queryFactory;

    QRole qRole = QRole.role;

    @Override
    public List<Role> getRoles() {
        return repository.findAll();
    }

    @Override
    public Page<Role> getRoles(GetRolesInput input) {
        Specification<Role> specification = Specifications.where(e -> {
            if (StringUtils.isNotBlank(input.getFilter())) {
                e.contains(Role.Fields.roleName, input.getFilter());
            }
        });

        return repository.findAll(specification, input.getPageable());
    }

    @Override
    public Role createOrUpdate(RoleCreateOrEditDto dto) {
        Role entity = null;
        if (dto.getId() != null) {
            entity = repository.findById(dto.getId()).orElseThrow(() -> new BusinessException("此角色不存在"));
            dozerMapper.map(dto, entity);
        } else {
            entity = dozerMapper.map(dto, Role.class);
        }
        validateRoleCodeAndNameUnique(entity);
        return repository.save(entity);
    }


    @Override
    public Role updatePermission(UpdateRolePermissionDto dto) {
        if (dto.getId() == null) {
            throw new BusinessException("ID不可为空");
        }
        Role entity = repository.findById(dto.getId()).orElseThrow(() -> new BusinessException("此角色不存在"));
        //modify by wangji 仅删除没有来源部分得
        queryFactory.delete(QGrantPermission.grantPermission).where(QGrantPermission.grantPermission.businessKey.isNull().and(QGrantPermission.grantPermission.role.id.eq(dto.getId()))).execute();

        entity.clearPermission();
        //dozerMapper.map(dto,entity);
        //modify by wangji 调整了方法
        entity.addPermissions(DozerUtils.mapList(dozerMapper, dto.getPermissions(), GrantPermission.class));

        entity.setPermissionRole();
        Role save = repository.save(entity);
        //刷新权限缓存
        permissionManager.refreshAll();
        return save;
    }

    @Override
    public RoleDto getRoleDetails(EntityDto dto) {
        if (dto.getId() == null) {
            throw new BusinessException("ID不可为空");
        }
        Role entity = repository.findById(dto.getId()).orElseThrow(() -> new BusinessException("此角色不存在"));
        RoleDto result = dozerMapper.map(entity, RoleDto.class);
        //筛选不包含标识得权限清单
        List<GrantPermission> orion = entity.getPermissions().stream().filter(p -> StrUtil.isEmpty(p.getBusinessKey())).collect(Collectors.toList());
        result.setPermissionList(getRoleRealPermission(orion));
        return result;
    }


    @Override
    public void deleteRole(EntityDto dto) {
        Role entity = repository.findById(dto.getId()).orElseThrow(() -> new BusinessException("此角色不存在"));
        repository.delete(entity);

        //刷新权限缓存
        permissionManager.refreshAll();
    }

    @Override
    public void exportRole(long roleId, OutputStream outputStream) {
        RoleDto roleDto = getRoleDetails(new EntityDto(roleId));
        List<RolePermissionDto> rolePermission = roleDto.getPermissionList();
        List<ExportRolePermissionDto> permissionList = rolePermission.stream().map(ExportRolePermissionDto::new).collect(Collectors.toList());
        List<ExportRolePermissionDto> clone = new ArrayList<>();
        clone.addAll(permissionList);
        permissionList.forEach(parent -> {
            var it = clone.iterator();
            while (it.hasNext()) {
                var next = it.next();
                if (StringUtils.equals(next.getParentName(), parent.getName())) {
                    parent.appendChild(next);
                    it.remove();
                }
            }
        });
        permissionList = permissionList.stream().filter(e -> StringUtils.isBlank(e.getParentName())).collect(Collectors.toList());
        clone.clear();
        permissionList.forEach(e -> {
            appendRolePermissionExcelDataList(clone, e, 0);
        });
        ExcelWriter excelWriter = null;
        InputStream templateStream = null;
        try {
            templateStream = ResourceUtil.getStream("templates/role-template.xlsx");
            excelWriter = EasyExcel.write(outputStream).withTemplate(templateStream).autoCloseStream(true).autoTrim(false)
                    .registerWriteHandler(new RolePermissionExportRowWriteHandler()).build();
            WriteSheet writeSheet = EasyExcel.writerSheet(0).needHead(Boolean.FALSE).build();
            excelWriter.fill(roleDto, writeSheet);
            excelWriter.fill(clone, writeSheet);

        } finally {
            if (excelWriter != null) {
                excelWriter.finish();
            }
            IOUtils.closeQuietly(templateStream);
        }
    }


    @Override
    public void importRole(RoleDto roleDto) {
        List<Role> roles = findAll(Specifications.where(e -> {
            e.eq(Role.Fields.roleName, roleDto.getRoleName());
        }));
        Role role = null;
        if (roles.isEmpty()) {
            role = new Role();
        } else {
            role = roles.get(0);
        }
        UtBeanUtils.copyNotNullProperties(roleDto, role);
        validateRoleCodeAndNameUnique(role);

        List<RolePermissionDto> permissionDtoList = roleDto.getPermissionList().stream().filter(e -> e.isGranted()).collect(Collectors.toList());
        Set<String> permissionSet = new HashSet<>();

        List<RolePermissionDto> tenantGrantedPermissions = getTenantGrantedPermissions();
        permissionDtoList.forEach(p -> {
            appendTenantPermission(p.getName(), permissionSet, tenantGrantedPermissions);
        });
        List<GrantPermission> deletePermissions = new ArrayList<>();
        var it = role.getPermissions().iterator();
        while (it.hasNext()) {
            boolean include = false;
            var next = it.next();
            var setIterator = permissionSet.iterator();
            while (setIterator.hasNext()) {
                if (StringUtils.equals(next.getName(), setIterator.next())) {
                    setIterator.remove();
                    include = true;
                    break;
                }
            }
            if (!include) {
                it.remove();
                deletePermissions.add(next);
            }
        }
        for (String s : permissionSet) {
            GrantPermission grantPermission = new GrantPermission();
            grantPermission.setRole(role);
            grantPermission.setName(s);
            role.addPermission(grantPermission);
        }
        permissionRepository.deleteInBatch(deletePermissions);
        save(role);
        //刷新权限缓存
        permissionManager.refreshAll();
    }

    @Override
    public void batchImportRole(List<RoleDto> roleDtoList) {
        if (roleDtoList == null || roleDtoList.isEmpty()) {
            return;
        }
        for (RoleDto roleDto : roleDtoList) {
            importRole(roleDto);
        }
    }


    @Override
    public Role getRoleByRoleName(String roleName) {
        return repository.findByRoleName(roleName).orElse(null);
    }

    @Override
    public List<Role> getRoleByIds(List<Long> roleIds) {
        return repository.findAllByIdIn(roleIds);
    }


    @Override
    public PagedResultDto<RoleListDto> getRoles(PagedAndSortedInputDto input) {
        Page<Role> pageResult = repository.findAll(input.getPageable());
        List<RoleListDto> list = DozerUtils.mapList(dozerMapper, pageResult.getContent(), RoleListDto.class);
        return new PagedResultDto<>(pageResult.getTotalElements(), list);
    }

    @Override
    public PagedResultDto<RoleListDto> getRolesForSync(PagedAndSortedInputDto input) {
        Page<Role> pageResult = repository.findAll(input.getPageable());
        List<RolePermissionDto> tenantGrantedPermissions = getTenantGrantedPermissions();
        List<RoleListDto> list = DozerUtils.mapList(dozerMapper, pageResult.getContent(), RoleListDto.class, (s, t) -> {
            List<RolePermissionDto> roleRealPermission = getRoleRealPermission(s.getPermissions(), tenantGrantedPermissions);
            t.setPermissionList(roleRealPermission);
        });
        return new PagedResultDto<>(pageResult.getTotalElements(), list);
    }

    @Override
    public Long getRoleIdByRoleName(String roleName) {
        Optional<Role> role = repository.findByRoleName(roleName);
        return role.map(BaseEntity::getId).orElse(null);
    }

    @Override
    public List<RoleListDto> getRolesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("参数ID不能为空");
        }
        List<Role> roles = queryFactory.selectFrom(QRole.role).where(QRole.role.id.in(ids)).fetch();
        List<RolePermissionDto> tenantGrantedPermissions = getTenantGrantedPermissions();
        return DozerUtils.mapList(dozerMapper, roles, RoleListDto.class, (s, t) -> {
            List<RolePermissionDto> roleRealPermission = getRoleRealPermission(s.getPermissions(), tenantGrantedPermissions);
            t.setPermissionList(roleRealPermission);
        });
    }

    @Override
    public List<RoleSimpleDto> getByRoleNames(List<String> roleNames) {
        QBean<RoleSimpleDto> qBean = QBeanBuilder.create(RoleSimpleDto.class).appendQEntity(qRole).build();
        return queryFactory.select(qBean).from(qRole).where(qRole.roleName.in(roleNames)).fetch();
    }

    @Override
    public List<RoleSimpleDto> getByRoleCodes(List<String> roleCodes) {
        QBean<RoleSimpleDto> qBean = QBeanBuilder.create(RoleSimpleDto.class).appendQEntity(qRole).build();
        return queryFactory.select(qBean).from(qRole).where(qRole.roleCode.in(roleCodes)).fetch();
    }

    @Override
    public List<RoleListDto> listDefaultRoles() {
        List<Role> roles = queryFactory.selectFrom(QRole.role).where(QRole.role.isDefault.eq(true)).fetch();
        return DozerUtils.mapList(dozerMapper, roles, RoleListDto.class);
    }

    @Override
    public List<RoleSimpleDto> getDefaultRoles() {
        QBean<RoleSimpleDto> qBean = QBeanBuilder.create(RoleSimpleDto.class).appendQEntity(qRole).build();
        return queryFactory.select(qBean).from(qRole).where(qRole.isDefault.eq(true)).fetch();
    }

    @Override
    public void bindRoleToUsers(BindRoleToUserInput input) {
        Role role = getOne(input.getRoleId());
        if (role == null) {
            throw new BusinessException("找不到对应角色！");
        }

        List<User> userList = userRepository.findAllById(input.getUserIds());
        userList = userList.stream().filter((e) -> role.getUserRoles().stream().noneMatch(u -> u.getId().equals(e.getId()))).collect(Collectors.toList());
        role.getUserRoles().addAll(userList);
        save(role);
    }

    @Override
    public void bindRoleToOrgans(BindRoleToOrgInput input) {
        Role role = getOne(input.getRoleId());
        if (role == null) {
            throw new BusinessException("找不到对应角色！");
        }
        //查找org
        List<Long> orgIds = input.getOrgIds();
        PagedAndSortedInputDto dto = new PagedAndSortedInputDto();
        dto.setUnPage(true);
        PagedResultDto<OrganizationUnitUserListDto> pagedResultDto = organizationUnitService
                .getOrganizationUnitUsers(dto, null, input.isIncludeChild(), orgIds.toArray(new Long[]{}));
        List<OrganizationUnitUserListDto> organs = pagedResultDto.getItems();
        //为org包含的user设置角色
        BindRoleToUserInput bindRoleToUserInput = new BindRoleToUserInput();
        bindRoleToUserInput.setRoleId(input.getRoleId());
        bindRoleToUserInput.setUserIds(organs.stream().map(OrganizationUnitUserListDto::getId).collect(Collectors.toList()));
        bindRoleToUsers(bindRoleToUserInput);
    }

    @Override
    public Page<User> getUserByRoleId(GetUserByRoleDto input) {
        if (input.getRoleId() == null) {
            throw new BusinessException("角色id不能为空!");
        }

        QUser q_user = QUser.user;
        JPAQuery<User> query = queryFactory.selectDistinct(q_user).from(q_user).where(q_user.userRoles.any().id.eq(input.getRoleId()));

        if (StrUtil.isNotBlank(input.getUserName())) {
            query.where(q_user.userName.containsIgnoreCase(input.getUserName().trim()).or(q_user.name.containsIgnoreCase(input.getUserName().trim())));
        }
        if (input.getUserType() != null) {
            query.where(q_user.userType.eq(input.getUserType()));
        }

        return (Page<User>) page(query, input.getPageable());
    }

    @Override
    public void unbindRoleToUser(UnbindRoleForUserDto roleForUserDto) {
        Role role = getOne(roleForUserDto.getRoleId());

        Iterator<User> iterator = role.getUserRoles().iterator();
        while (iterator.hasNext()) {

            for (Long userId : roleForUserDto.getUserIds()) {
                if (userId.equals(iterator.next().getId())) {
                    iterator.remove();
                }
            }
        }

        save(role);
    }

    @Override
    public List<Long> getDefaultRoleIds() {
        return queryFactory.select(QRole.role.id).from(QRole.role).where(QRole.role.isDefault.eq(Boolean.TRUE)).fetch();
    }

    @Override
    public void deleteByCode(String code) {
        long i = queryFactory.delete(QRole.role).where(QRole.role.roleCode.eq(code)).execute();
        if (i > 0) {
            //刷新权限缓存
            permissionManager.refreshAll();
        }
    }

    @Override
    public List<Role> findByCodes(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return new ArrayList<>();
        }
        return queryFactory.selectFrom(QRole.role).where(QRole.role.roleCode.in(codes)).fetch();
    }


    @Override
    public Long totalCount() {
        return count();
    }


    //region private method

    /**
     * 获取租户或者宿主的全部授权权限
     *
     * @return
     */
    private List<RolePermissionDto> getTenantGrantedPermissions() {
        List<RolePermissionDto> result = new ArrayList<>();
        //租户角色权限清单
        if (TenantContextHolder.getTenantId() == null) {
            List<Permission> permissions = permissionManager.getDefinitionPermissionTree(false);
            permissions.forEach(s -> {
                RolePermissionDto rolePermissionDto = dozerMapper.map(s, RolePermissionDto.class);
                rolePermissionDto.setGranted(true);
                result.add(rolePermissionDto);
            });
        } else {
            List<RolePermissionDto> tenantPermissions = tenantPermissionService.getTenantPermissions(TenantContextHolder.getTenantId());
            result.addAll(tenantPermissions.stream().filter(RolePermissionDto::isGranted).collect(Collectors.toList()));
        }
        return result;
    }

    /**
     * 获取真实的角色权限清单
     *
     * @param rolePermission   角色数据库保存的权限
     * @param tenantPermission 租户权限
     * @return
     */
    private List<RolePermissionDto> getRoleRealPermission(List<GrantPermission> rolePermission, List<RolePermissionDto> tenantPermission) {
        List<RolePermissionDto> result = new ArrayList<>();
        List<String> granted = rolePermission.stream().map(s -> s.getName()).collect(Collectors.toList());
        tenantPermission.forEach(s -> {
            RolePermissionDto dto = new RolePermissionDto();
            dto.setGranted(granted.contains(s.getName()));
            dto.setDisplayName(s.getDisplayName());
            dto.setName(s.getName());
            dto.setParentName(s.getParentName());
            result.add(dto);
        });
        return result;
    }

    /**
     * 获取真实的角色权限清单
     *
     * @param rolePermission 角色数据库保存的权限
     * @return
     */
    private List<RolePermissionDto> getRoleRealPermission(List<GrantPermission> rolePermission) {
        return getRoleRealPermission(rolePermission, getTenantGrantedPermissions());
    }

    void validateRoleCodeAndNameUnique(Role role) {
        QRole qRole = QRole.role;
        JPAQuery<Role> query = queryFactory.selectFrom(qRole).where(qRole.roleCode.eq(role.getRoleCode()));
        if (role.getId() != null) {
            query.where(qRole.id.ne(role.getId()));
        }
        long count = query.fetchCount();
        if (count > 0) {
            throw new BusinessException("角色代码已存在");
        }
        query = queryFactory.selectFrom(qRole).where(qRole.roleName.eq(role.getRoleName()));
        if (role.getId() != null) {
            query.where(qRole.id.ne(role.getId()));
        }
        count = query.fetchCount();
        if (count > 0) {
            throw new BusinessException("角色名称已存在");
        }
    }

    void appendRolePermissionExcelDataList(List<ExportRolePermissionDto> dataList, ExportRolePermissionDto permission, int level) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4 * level; i++) {
            sb.append(" ");
        }
        permission.setDisplayName(sb.toString() + permission.getDisplayName());
        dataList.add(permission);
        for (ExportRolePermissionDto child : permission.getChildren()) {
            appendRolePermissionExcelDataList(dataList, child, level + 1);
        }
    }

    void appendTenantPermission(String permission, Set<String> permissionSet, List<RolePermissionDto> tenantPermissions) {
        var it = tenantPermissions.iterator();
        while (it.hasNext()) {
            var next = it.next();
            if (StringUtils.equals(permission, next.getName())) {
                permissionSet.add(permission);
                if (StringUtils.isNotBlank(next.getParentName())) {
                    appendTenantPermission(next.getParentName(), permissionSet, tenantPermissions);
                }
                break;
            }
        }
    }
    //end region


    @Override
    public RoleSimpleDto getRoleSimple(long roleId) {
        return queryFactory.select(createRoleSimpleQBean()).from(qRole).where(qRole.id.eq(roleId)).fetchOne();
    }

    @Override
    public List<RoleSimpleDto> getRoleSimple(Long... roleId) {
        if (Objects.isNull(roleId) || roleId.length == 0) {
            return new ArrayList<>();
        }
        List<RoleSimpleDto> result;
        if (roleId.length == 1) {
            result = new ArrayList<>();
            RoleSimpleDto dto = getRoleSimple(roleId[0]);
            if (!Objects.isNull(dto)) {
                result.add(dto);
            }
        } else {
            result = queryFactory.select(createRoleSimpleQBean()).from(qRole).where(qRole.id.in(roleId)).fetch();
        }
        return result;
    }

    @Override
    public List<RoleSimpleDto> getRoleSimple(Collection<Long> roleIds) {
        if (Objects.isNull(roleIds) || roleIds.size() == 0) {
            return new ArrayList<>();
        }
        List<RoleSimpleDto> result;
        if (roleIds.size() == 1) {
            result = new ArrayList<>();
            RoleSimpleDto dto = getRoleSimple(roleIds.iterator().next());
            if (!Objects.isNull(dto)) {
                result.add(dto);
            }
        } else {
            result = queryFactory.select(createRoleSimpleQBean()).from(qRole).where(qRole.id.in(roleIds)).fetch();
        }
        return result;
    }

    QBean<RoleSimpleDto> createRoleSimpleQBean() {
        return QBeanBuilder.create(RoleSimpleDto.class).appendQEntity(qRole).build();
    }
}
