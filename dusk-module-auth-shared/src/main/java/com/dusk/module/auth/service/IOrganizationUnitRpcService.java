package com.dusk.module.auth.service;

import com.dusk.common.core.dto.PagedAndSortedInputDto;
import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.module.auth.dto.orga.OrganizationUnitDto;
import com.dusk.module.auth.dto.orga.OrganizationUnitUserListDto;

import java.util.List;
import java.util.Map;

/**
 * @author kefuming
 * @date 2020-07-24 17:47
 */
public interface IOrganizationUnitRpcService {
    List<OrganizationUnitDto> getAllOrgas();

    OrganizationUnitDto findOneByDisplayName(String displayName);

    OrganizationUnitDto findOneById(Long id);

    List<OrganizationUnitDto> findByIds(List<Long> ids);

    List<OrganizationUnitDto> getOrganizationUnitsByUserId(Long userId);

    OrganizationUnitDto getCurrentOrganization();

    PagedResultDto<OrganizationUnitUserListDto> getOrganizationUnitUsers(PagedAndSortedInputDto pageReq, String filter, boolean deepQuery, Long... orgIds);

    List<OrganizationUnitDto> saveAllOrgas(List<OrganizationUnitDto> orgList);

    List<OrganizationUnitDto> getStationsByCurrUserAndStation(Long userId);

    List<OrganizationUnitDto> getStationsByParentId(Long orgId);

    default List<OrganizationUnitDto> getOrganizationUnitsByType(String type) {
        throw new UnsupportedOperationException("暂不持支，请升级auth服务版本");
    }

    /**
     * 基于所属的组织id和用户名称模糊查询获取用户id列表
     */
    default List<Long> getUserIdsByOrgIdAndNameLike(String name, Long orgId, Boolean deepQuery) {
        throw new UnsupportedOperationException("暂不持支，请升级auth服务版本");
    }

    default Map<Long, List<OrganizationUnitDto>> getOrganizationUnitMapByOrgIds(List<Long> orgIds) {
        throw new UnsupportedOperationException("暂不持支，请升级auth服务版本");
    }

}
