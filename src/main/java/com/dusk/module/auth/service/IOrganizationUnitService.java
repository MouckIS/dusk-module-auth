package com.dusk.module.auth.service;

import com.dusk.module.auth.dto.orga.*;
import com.dusk.common.framework.dto.EntityDto;
import com.dusk.common.framework.dto.ListResultDto;
import com.dusk.common.framework.service.IBaseService;
import com.dusk.common.module.auth.dto.orga.GetOrganizationUnitUsersInput;
import com.dusk.common.module.auth.dto.orga.OrganizationUnitDto;
import com.dusk.common.module.auth.dto.orga.OrganizationUnitUserListDto;
import com.dusk.common.module.auth.service.IOrganizationUnitRpcService;
import com.dusk.module.auth.dto.orga.*;
import com.dusk.module.auth.dto.station.StationsOfLoginUserDto;
import com.dusk.module.auth.entity.OrganizationUnit;
import com.dusk.module.auth.entity.User;
import com.dusk.module.auth.repository.IOrganizationUnitRepository;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author kefuming
 * @date 2020-05-13 13:41
 */
public interface IOrganizationUnitService extends IBaseService<OrganizationUnit, IOrganizationUnitRepository>, IOrganizationUnitRpcService {

    /**
     * 获取外单位的所有组织
     * @return
     */
    ListResultDto<OrganizationStationUnitDto> getExternalOrganizationUnits();

    /**
     * 获取组织机构下的用户
     *
     * @param input
     * @return
     */
    Page<OrganizationUnitUserListDto> getOrganizationUnitUsers(GetOrganizationUnitUsersInput input);

    /**
     * 获取组织机构下的用户(部分信息)
     *
     * @param input
     * @return
     */

    Page<OrganizationUnitUserInfoListDto> getOrganizationUnitUsersInfo(GetOrganizationUnitUsersExtInput input);

    /**
     * 获取组织机构的用户下拉列表
     *
     * @param input
     * @return
     */
    Page<OrganizationUnitUserForSelectDto> getOrganizationUnitUsersForSelect(GetOrganizationUnitUsersForSelectInput input);

    /**
     * 创建组织机构
     *
     * @param input
     * @return
     */
    OrganizationUnit create(CreateOrganizationUnitInput input);

    /**
     * 创建外单位组织机构
     * @param input
     * @return
     */
    OrganizationUnit createExternalOrganization(CreateOrganizationUnitInput input);

    /**
     * 更新组织机构
     *
     * @param input
     * @return
     */
    OrganizationUnit update(UpdateOrganizationUnitInput input);

    /**
     * 移动组织机构
     *
     * @param input
     * @return
     */
    OrganizationUnit move(MoveOrganizationUnitInput input);

    /**
     * 级联删除子节点
     * @param orgId
     */
    void deleteOrgById(Long orgId);

    /**
     * 从组织机构中删除用户
     *
     * @param input
     */
    void removeUserFromOrganizationUnit(UserToOrganizationUnitInput input);

    /**
     * 添加用户到组织机构
     *
     * @param input
     */
    void addUsersToOrganizationUnit(UsersToOrganizationUnitInput input);

    /**
     * 根据用户ID获取该用户所属的所有组织机构
     *
     * @param input
     * @return
     */
    List<OrganizationUnit> getOrganizationUnitsByUser(EntityDto input);

    /**
     * 获取组织机构节点的父级[包括自身]
     *
     * @param input
     * @return
     */
    List<OrganizationUnit> getParentOrganizations(EntityDto input);

    /**
     * 获取厂站
     *
     * @return
     */
    List<OrganizationUnit> getStations();

    /**
     * 获取组织机构下的用户
     *
     * @param input
     * @return
     */
    Page<User> findUsers(GetOrganizationUnitUsersInput input);

    /**
     * 根据用户id获取用户的 并且只获取有效的
     *
     * @param id
     * @return
     */
    List<OrganizationUnit> getStationsByUserId(Long id);


    /**
     * 根据用户id获取用户的厂站  获取所有的
     * @param id
     * @return
     */
    List<OrganizationUnit> getAllStationsByUserId(Long id);
    /**
     * 给前端右上角用的厂站列表，包含默认厂站
     *
     * @param id
     * @return
     */
    List<StationsOfLoginUserDto> getStationsForFrontByUserId(Long id);

    /**
     * 获取当前用户下的所有厂站-（包含普通节点的最小树结构）
     *
     * @param id
     * @return
     */
    List<OrganizationUnit> getStationTreeByUserId(Long id);

    /**
     * 获取当前机构下面的厂站
     *
     * @param orgId
     * @return
     */
    List<OrganizationUnitDto> getStationsByParentId(Long orgId);

    /**
     * 根据编码查询
     * @param codes
     * @return
     */
    List<OrganizationUnit> findByCodes(List<String> codes);

    void deleteByCodes(List<String> deleteBizOrgIds);

    //设置厂站可用/不可用
    void setStationEnabled(Long id,boolean enabled);

    void importUnitByExcel(MultipartFile file);
}
