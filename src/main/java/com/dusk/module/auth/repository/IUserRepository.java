package com.dusk.module.auth.repository;

import com.dusk.common.framework.repository.IBaseRepository;
import com.dusk.common.module.auth.enums.EUnitType;
import com.dusk.module.auth.dto.user.UserIdAndPermissionDto;
import com.dusk.module.auth.entity.User;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * @author kefuming
 * @date 2020-04-28 10:05
 */
public interface IUserRepository extends IBaseRepository<User> {

    //@Query("select u from User u left join fetch u.userRoles ur left join fetch u.organizationUnit uo where u.userName = ?1")
    @EntityGraph(value = "user.role")
    Optional<User> findByUserName(String username);

    @EntityGraph(value = "user.role")
    Optional<User> findByUserNameIgnoreCase(String username);

    @EntityGraph(value = "user.role")
    List<User> findByUserNameAndTenantIdIsNotNull(String username);

    @EntityGraph(value = "user.role")
    List<User> findByUserNameAndTenantId(String username, Long tenantId);

    @EntityGraph(value = "user.role")
    List<User> findByPhoneNo(String phoneNo);

    @EntityGraph(value = "user.role")
    List<User> findByPhoneNoAndTenantId(String phoneNo, Long tenantId);

    @EntityGraph(value = "user.role")
    List<User> findByIdIn(List<Long> userIds);

    @EntityGraph(value = "user.role")
    List<User> findAll(Sort sort);

    List<User> findByName(String name);

    List<User> findByNameIn(List<String> nameList);

    @Override
    Optional<User> findById(Long userid);

    Optional<User> findByWorkNumber(String workNumber);

    void deleteByIdIn(List<Long> ids);

    @Query("select distinct a.id from User a inner join a.userRoles b inner join b.permissions c where c.name in (:permissions) and a.userType in (:userTypes) and (a.userStatus = 'OnJob')")
    List<Long> getUserIdsByPermissions(String[] permissions, List<EUnitType> userTypes);

    @Query("select new com.dusk.module.auth.dto.user.UserIdAndPermissionDto(a.id,c.name) from User a inner join a.userRoles b inner join b.permissions c where c.name in (:permissions) and a.userType in (:userTypes) and (a.userStatus = 'OnJob')")
    List<UserIdAndPermissionDto> getUserIdsByPermissionsAnd(String[] permissions, List<EUnitType> userTypes);
}
