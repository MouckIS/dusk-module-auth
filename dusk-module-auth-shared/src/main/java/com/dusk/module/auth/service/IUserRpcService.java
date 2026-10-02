package com.dusk.module.auth.service;

import com.dusk.common.core.dto.PagedAndSortedInputDto;
import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.common.core.enums.EUnitType;
import com.dusk.module.auth.dto.CreateOrUpdateUserInput;
import com.dusk.module.auth.dto.GetUsersByOrgInput;
import com.dusk.module.auth.dto.UserFullListDto;
import com.dusk.module.auth.dto.UserFullListSyncDto;
import com.dusk.module.auth.dto.UserInputDto;
import com.dusk.module.auth.dto.UserOrgDto;
import com.dusk.module.auth.dto.UserSimpleDto;
import com.dusk.module.auth.dto.orga.OrganizationUnitDto;
import com.dusk.module.auth.dto.orga.OrganizationUnitUserDto;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author kefuming
 * @date 2020-07-24 14:41
 */
public interface IUserRpcService {
    /**
     * 查询用户列表
     *
     * @param input 检索条件
     * @return PagedResultDto<UserFullListDto>
     */
    PagedResultDto<UserFullListDto> getUsers(PagedAndSortedInputDto input);

    /**
     * 获取用户列表
     *
     * @param useInput 查询用户列表的实体类
     * @return Page<UserFullListDto>
     */
    PagedResultDto<UserFullListDto> getUsers(UserInputDto useInput);

    /**
     * 同步用户列表
     *
     * @param input
     * @return
     */
    PagedResultDto<UserFullListSyncDto> getUsersForSync(PagedAndSortedInputDto input);

    /**
     * 根据用户id查找用户
     *
     * @param userId
     * @return
     */
    UserFullListDto getUserFullById(Long userId);

    /**
     * 查询用户的上级
     */
    default UserFullListDto getSuperiorUserFullById(Long userId) {
        throw new UnsupportedOperationException("暂不持支，请升级auth服务版本");
    }

    /**
     * 查询用户的上级
     */
    default Long getSuperiorId(Long userId) {
        throw new UnsupportedOperationException("暂不持支，请升级auth服务版本");
    }

    /**
     * 批量获取用户信息
     *
     * @param userIds
     * @return
     */
    List<UserFullListDto> getUserFullByIds(List<Long> userIds);

    /**
     * 批量获取用户信息 （包括用户组织信息）
     *
     * @param userIds
     * @return
     */
    default List<UserOrgDto> getUserOrgByIds(List<Long> userIds) {
        throw new UnsupportedOperationException("暂不持支，请升级auth服务版本");
    }


    /**
     * 获取指定id的拥有的权限清单
     *
     * @param userId
     * @return
     */
    List<String> getUserPermissions(Long userId);


    /**
     * 基于用户名称模糊查询获取用户id列表
     *
     * @param name
     * @return
     */
    List<Long> getUserIdsByNameLike(String name);

    /**
     * 基于用户名的前面字符模糊查询获取用户列表
     *
     * @param head
     * @return
     */
    List<UserFullListDto> getUsersByUserNameStartWith(String head);

    List<UserFullListDto> getUsersByUserNameStartWith(String head, List<EUnitType> userTypes);


    /**
     * 根据权限名获取用户id列表 所有权限均匹配
     *
     * @param permissions
     * @return
     */
    List<Long> getUserIdsByPermissionsAnd(String[] permissions);

    /**
     * 根据权限名获取用户id列表 符合其中一个权限的
     *
     * @param permissions
     * @return
     */
    List<Long> getUserIdsByPermissionsOr(String[] permissions);

    List<Long> getUserIdsByPermissionsOr(String[] permissions, List<EUnitType> userTypes);

    List<Long> getUserIdsByPermissionsAnd(String[] permissions, List<EUnitType> userTypes);


    /**
     * 根据指定权限名获取所有包含权限清单的用户
     *
     * @param permission
     * @return
     */
    List<Long> getUserIdsByPermission(String permission);

    List<Long> getUserIdsByPermission(String permission, List<EUnitType> userTypes);


    /**
     * 判断指定用户id是否存在以下任意一个权限
     *
     * @param userId
     * @param permissions
     * @return
     */
    boolean checkUserContainOneOfPermissions(Long userId, String[] permissions);


    /**
     * 判断指定用户id是否存在以下所有权限
     *
     * @param userId
     * @param permissions
     * @return
     */
    boolean checkUserContainPermissions(Long userId, String[] permissions);

    /**
     * 判断指定用户id是否存在以下所有权限
     *
     * @param userId
     * @param permission
     * @return
     */
    boolean checkUserContainPermission(Long userId, String permission);


    /**
     * 对密码明文进行加密
     *
     * @param password
     * @return
     */
    String encodePassword(String password);

    Long createOrUpdateUser(CreateOrUpdateUserInput createOrUpdateUserInput);

    void createOrUpdateUsers(List<CreateOrUpdateUserInput> inputList);

    /**
     * @Description 根据用户名查找用户
     * @date 2021/1/21 15:18
     */
    UserFullListDto getUserByUserName(String userName);

    /**
     * @Description 根据姓名查找用户
     * @date 2021/1/21 15:18
     */
    UserFullListDto getUserByName(String name);

    List<UserFullListDto> getUsersContainsName(String name);

    List<UserFullListDto> getUsersContainsName(String name, List<EUnitType> userTypes);

    UserFullListDto getUserByWorkNumber(String workNumber);

    /**
     * 同步整个域用户信息
     *
     * @param saveUserList
     * @param saveOrgList
     * @param saveOrgUserList
     */
    void syncUserAndOrg(List<UserSimpleDto> saveUserList, List<OrganizationUnitDto> saveOrgList, List<OrganizationUnitUserDto> saveOrgUserList, List<Long> resignedUserIds);


    void syncUserByWeChatFromLdap(UserSimpleDto userDto, List<OrganizationUnitDto> employeeOrgList, OrganizationUnitUserDto orgUserDto);


    List<UserSimpleDto> loadAllUserList();

    UserSimpleDto getUserSimpleDto(Long userId);

    List<UserSimpleDto> getUserSimpleDto(Long... userId);

    List<UserSimpleDto> getUserSimpleDto(Collection<Long> userId);

    List<UserFullListDto> getUsersByRoleName(String roleName);

    List<UserFullListDto> getUsersByRoleName(String roleName, boolean filterByStation);

    List<UserFullListDto> getUsersByRoleName(String roleName, List<EUnitType> userTypes);

    List<UserFullListDto> getUsersByRoleName(String roleName, boolean filterByStation, List<EUnitType> userTypes);

    String generateToken(Long userId);

    // 根据邮箱或手机号查找用户
    UserFullListDto getByEmailAddressOrPhoneNo(String input);


    /**
     * 根据用户名生成登录token
     *
     * @param userName
     */
    String generateTokenByUserName(String userName);

    /**
     * 根据用户名生成登录token,并设置过期时间
     */
    default String generateExpireTokenByUserName(String userName, long expireTime, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    /**
     * 根据角色ID列表获取用户列表
     *
     * @param roleIds
     * @return
     */
    List<UserFullListDto> getUsersByRoleIds(List<Long> roleIds);

    void deleteUser(Long id);

    // 优特云锁域账号同步
    void syncFromLdap(List<UserSimpleDto> saveUserList, List<String> resignedUserNames, Map<String, List<OrganizationUnitDto>> userOrgMap);

    List<Long> getCurrentRoles(Long userId);

    List<UserFullListDto> getUsersByOrg(GetUsersByOrgInput input);

    /**
     * 清除用户token，使用户强制下线
     */
    default void removeToken(String authorization) {
        throw new UnsupportedOperationException();
    }

    Long totalCount();
}
