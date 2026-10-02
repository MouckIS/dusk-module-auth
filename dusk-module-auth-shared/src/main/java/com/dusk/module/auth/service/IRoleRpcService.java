package com.dusk.module.auth.service;

import com.dusk.common.core.dto.PagedAndSortedInputDto;
import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.module.auth.dto.RoleSimpleDto;
import com.dusk.module.auth.dto.role.RoleListDto;

import java.util.Collection;
import java.util.List;

/**
 * @author kefuming
 * @date 2020-07-24 17:27
 */
public interface IRoleRpcService {
    /**
     * 同步用接口
     *
     * @param input
     * @return
     */
    PagedResultDto<RoleListDto> getRoles(PagedAndSortedInputDto input);

    PagedResultDto<RoleListDto> getRolesForSync(PagedAndSortedInputDto input);

    Long getRoleIdByRoleName(String roleName);

    List<RoleListDto> getRolesByIds(List<Long> ids);

    /**
     * 根据角色名查找角色
     *
     * @param roleNames
     * @return
     */
    List<RoleSimpleDto> getByRoleNames(List<String> roleNames);

    List<RoleSimpleDto> getByRoleCodes(List<String> roleCodes);

    RoleSimpleDto getRoleSimple(long roleId);

    List<RoleSimpleDto> getRoleSimple(Long... roleId);

    List<RoleSimpleDto> getRoleSimple(Collection<Long> roleIds);

    /**
     * @return
     */
    List<RoleSimpleDto> getDefaultRoles();

    List<RoleListDto> listDefaultRoles();
}
