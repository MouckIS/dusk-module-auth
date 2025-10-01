package com.dusk.module.auth.repository;

import com.dusk.common.rpc.auth.dto.orga.OrganizationUnitUserListDto;
import com.dusk.common.core.annotation.DisableGlobalFilter;
import com.dusk.common.core.enums.EUnitType;
import com.dusk.common.core.repository.IBaseRepository;
import com.dusk.module.auth.dto.orga.OrganizationUnitUserForSelectDto;
import com.dusk.module.auth.dto.orga.OrganizationUnitUserInfoListDto;
import com.dusk.module.auth.entity.OrganizationUnit;
import com.dusk.module.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

/**
 * @author kefuming
 * @date 2020-05-13 13:39
 */
public interface IOrganizationUnitRepository extends IBaseRepository<OrganizationUnit> {
    /**
     * 查询组织机构下的用户，queryOrgaIds为-1时查询所有用户
     * @param queryOrgaIds
     * @param filter
     * @param pageable
     * @return
     */
    @Query("select new com.dusk.common.rpc.auth.dto.orga.OrganizationUnitUserListDto(" +
            "u.id, u.name, u.userName, u.emailAddress, orga.id, orga.displayName) " +
            "from OrganizationUnit orga inner join orga.users u " +
            "where (-1L in (:queryOrgaIds) or orga.id in (:queryOrgaIds)) " +
            "and (:filter is null or :filter = '' or u.name like %:filter% or u.userName like %:filter%)" +
            "and (:type is null or orga.type = :type)" +
            "and (u.userStatus = 'OnJob')")
    Page<OrganizationUnitUserListDto> getOrganizationUnitUsers(@Param("queryOrgaIds") Set<Long> queryOrgaIds, @Param("filter") String filter, Pageable pageable, EUnitType type);
    @Query("select new com.dusk.module.auth.dto.orga.OrganizationUnitUserInfoListDto(" +
            "u.id, u.name, u.userName, u.emailAddress, orga.id, orga.displayName, u.job, u.phoneNo, u.userStatus) " +
            "from OrganizationUnit orga inner join orga.users u " +
            "where (-1L in (:queryOrgaIds) or orga.id in (:queryOrgaIds)) " +
            "and (:filter is null or :filter = '' or u.name like %:filter% or u.userName like %:filter%)" +
            "and (:type is null or orga.type = :type)" +
            "and (u.userStatus = 'OnJob')")
    Page<OrganizationUnitUserInfoListDto> getOrganizationUnitUsersInfo(@Param("queryOrgaIds") Set<Long> queryOrgaIds, @Param("filter") String filter, Pageable pageable, EUnitType type);

    @Query("select distinct new com.dusk.module.auth.dto.orga.OrganizationUnitUserForSelectDto(u.id,u.name,u.userName) from User u where u.id not in (select u2.id from OrganizationUnit orga inner join orga.users u2 where orga.id = :orgId) and (:filter is null or :filter = '' or u.name like %:filter% or u.userName like %:filter%) and u.userType = 'Inner' and u.organizationUnit is empty")
    Page<OrganizationUnitUserForSelectDto> getOrganizationUnitUsersForSelect(@Param("orgId") Long orgId, @Param("filter") String filter, Pageable pageable);

    @Query("select orga from OrganizationUnit orga inner join orga.users u where u.id = ?1 ")
    List<OrganizationUnit> getOrganizationUnitsByUser(Long userId);

    List<OrganizationUnit> findByStation(boolean station);

    List<OrganizationUnit> findByStationAndStationEnabled(boolean station,boolean stationEnabled);

    @DisableGlobalFilter
    List<OrganizationUnit> findAllByDr(int dr);

    /**
     * 查询组织机构下的用户，queryOrgaIds为-1时查询所有用户
     * @param queryOrgaIds
     * @param filter
     * @param pageable
     * @return
     */
    @Query("select distinct u from OrganizationUnit orga inner join orga.users u " +
            "where (-1L in (:queryOrgaIds) or orga.id in (:queryOrgaIds)) " +
            "and (:filter is null or :filter = '' or u.name like %:filter% or u.userName like %:filter%)" +
            "and (:unitType is null or orga.type = :unitType)" +
            "and (u.userStatus = 'OnJob')")
    Page<User> findUsers(@Param("queryOrgaIds") Set<Long> queryOrgaIds, @Param("filter") String filter, Pageable pageable, EUnitType unitType);

    /**
     * 基于组织id和用户名称模糊查询获取用户id列表
     * @param queryOrgaIds 查询的组织id
     * @param name 模糊查询用户名称
     * @param orgaType 组织类型
     */
    @Query("select u.id from OrganizationUnit orga inner join orga.users u " +
            "where ((:queryOrgaIds) is null or orga.id in (:queryOrgaIds)) " +
            "and (:name is null or :name = '' or u.name like %:name% )" +
            "and (:orgaType is null or orga.type = :orgaType)" +
            "and (u.userStatus = 'OnJob')")
    List<Long> getUserIds(@Param("queryOrgaIds") Set<Long> queryOrgaIds, @Param("name") String name, EUnitType orgaType);

    @Query("select org from OrganizationUnit org inner join org.users u on u.id = :userId")
    List<OrganizationUnit> getStationsByCurrUserAndStation(@Param("userId") Long userId);

    @Query("select org from OrganizationUnit org where org.parentId = :parentId")
    List<OrganizationUnit> getStationsByParentId(Long parentId);

    List<OrganizationUnit> findByIdIn(List<Long> ids);

    @Query("select org from OrganizationUnit org where org.type = :type")
    List<OrganizationUnit> getOrganizationUnitsByType(EUnitType type);

    @Query("select distinct u from OrganizationUnit orga inner join orga.users u " +
            "where orga.id = :orgId and u.userStatus = 'OnJob' order by u.userName asc")
    List<User> findUsersByOrgId(@Param("orgId") Long orgId);

}
