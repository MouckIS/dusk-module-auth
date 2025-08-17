package com.dusk.module.auth.service;


import com.dusk.common.framework.dto.EntityDto;
import com.dusk.common.framework.service.IBaseService;
import com.dusk.common.module.auth.service.IRoleRpcService;
import com.dusk.module.auth.dto.orga.BindRoleToOrgInput;
import com.dusk.module.auth.dto.role.GetRolesInput;
import com.dusk.module.auth.dto.role.RoleCreateOrEditDto;
import com.dusk.module.auth.dto.role.RoleDto;
import com.dusk.module.auth.dto.role.UpdateRolePermissionDto;
import com.dusk.module.auth.dto.user.BindRoleToUserInput;
import com.dusk.module.auth.dto.user.GetUserByRoleDto;
import com.dusk.module.auth.dto.user.UnbindRoleForUserDto;
import com.dusk.module.auth.entity.Role;
import com.dusk.module.auth.entity.User;
import com.dusk.module.auth.repository.IRoleRepository;
import org.springframework.data.domain.Page;

import java.io.OutputStream;
import java.util.List;

public interface IRoleService extends IBaseService<Role, IRoleRepository>, IRoleRpcService {

    /**
     * 获取所有角色
     *
     * @return
     */
    List<Role> getRoles();

    /**
     * 获取所有角色(分页)
     *
     * @param input
     * @return
     */
    Page<Role> getRoles(GetRolesInput input);

    /**
     * 新增或修改角色
     *
     * @param dto
     * @return
     */
    Role createOrUpdate(RoleCreateOrEditDto dto);

    /**
     * 修改权限
     *
     * @param dto
     * @return
     */
    Role updatePermission(UpdateRolePermissionDto dto);

    /**
     * 获取角色的详细内容
     *
     * @param dto
     * @return
     */
    RoleDto getRoleDetails(EntityDto dto);

    /**
     * 删除角色
     *
     * @param dto
     */
    void deleteRole(EntityDto dto);

    void exportRole(long roleId, OutputStream outputStream);

    void importRole(RoleDto roleDto);

    void batchImportRole(List<RoleDto> roleDtoList);

    /**
     * 根据角色名查找角色
     *
     * @param roleName
     * @return
     */
    Role getRoleByRoleName(String roleName);

    /**
     * 根据角色ID列表获取角色列表
     *
     * @param roleIds
     * @return
     */
    List<Role> getRoleByIds(List<Long> roleIds);

    /**
     * 为角色关联用户列表
     */
    void bindRoleToUsers(BindRoleToUserInput input);

    /**
     * 为角色关联组织下的用户
     *
     * @param input
     */
    void bindRoleToOrgans(BindRoleToOrgInput input);

    Page<User> getUserByRoleId(GetUserByRoleDto input);

    void unbindRoleToUser(UnbindRoleForUserDto roleForUserDto);

    /**
     * 获取默认得角色id
     *
     * @return
     */
    List<Long> getDefaultRoleIds();

    void deleteByCode(String code);

    List<Role> findByCodes(List<String> codes);
}
