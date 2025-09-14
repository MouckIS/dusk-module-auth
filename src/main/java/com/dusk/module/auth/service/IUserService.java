package com.dusk.module.auth.service;


import com.dusk.module.auth.dto.user.*;
import com.dusk.common.framework.dto.EntityDto;
import com.dusk.common.framework.dto.PagedResultDto;
import com.dusk.common.framework.model.UserContext;
import com.dusk.common.module.auth.dto.ChangePwdInput;
import com.dusk.common.module.auth.dto.CreateOrUpdateUserInput;
import com.dusk.common.module.auth.dto.UserFullListDto;
import com.dusk.module.auth.dto.user.*;
import com.dusk.module.auth.entity.Role;
import com.dusk.module.auth.entity.User;
import org.springframework.data.domain.Page;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author kefuming
 * @date 2020/5/15 11:56
 */
public interface IUserService {
    /**
     * 验证并获取用户
     *
     * @param username 用户名
     * @param password 密码
     * @return User
     */
    User checkAndGetUser(String username, String password);

    /**
     * 获取用户列表
     *
     * @param getUserInput 查询用户列表的实体类
     * @return Page<User>
     */
    Page<User> getUsers(GetUsersInput getUserInput);

    /**
     * 获取用户列表
     *
     */
    PagedResultDto<UserListDto> getUsersList(GetUsersInput getUserInput);

    /**
     * 获取指定组织机构的用户列表
     *
     * @param getOrgaUsersInput 查询用户列表的实体类
     * @return Page<User>
     */
    Page<User> getOrgaUsers(GetOrgaUsersInput getOrgaUsersInput);

    /**
     * 通过角色Code列表获取用户
     *
     * @param getUserInput 通过角色Code列表查询用户列表的实体类
     * @return Page<User>
     */
    Page<User> getUsersByRoleCodes(GetUsersByRoleCodesInput getUserInput);


    /**
     * 通过角色name列表获取用户
     *
     * @param input 通过角色name列表查询用户列表的实体类
     * @return Page<User>
     */
    Page<User> getUsersByRoleName(GetUsersByRoleNameInput input);

    /**
     * 获取所有用户列表
     *
     * @return List<User>
     */
    List<User> getAllUsers();

    /**
     * 获取所有本单位用户列表
     * @return
     */
    List<User> getAllInnerUsers();

    /**
     * 根据ID获取用户
     *
     * @param userId 用户id
     * @return User
     */
    User getUserById(Long userId);

    /**
     * 导出用户
     */
    void getUsersToExcel(HttpServletResponse response);

    /**
     * 获取单个用户用于编辑
     *
     * @param entityDto 用户id
     * @return GetUserForEditOutput
     */
    GetUserForEditOutput getUserForEdit(EntityDto entityDto);

    /**
     * 获取单个外单位用户用于编辑
     */
    GetExternalUserEditOutput getExternalUserEditInfo(Long userId);


    GetUserInfoOutput getUserInfo(Long id);

    /**
     * 获取上级
     */
    UserFullListDto getSuperiorUserFullById(Long userId);

    /**
     * 获取上级id
     */
    Long getSuperiorId(Long userId);

    void updatePersonalInfo(PersonalInfoInput input);

    /**
     * 创建/修改用户
     *
     * @param createOrUpdateUserInput 用户信息
     */
    Long createOrUpdateUser(CreateOrUpdateUserInput createOrUpdateUserInput);

    /**
     * 创建/修改用户
     *
     * @param createOrUpdateUserInput 用户信息
     */
    void createOrUpdateUserExistByUserName(CreateOrUpdateUserInput createOrUpdateUserInput);

    /**
     * 创建/修改用户 （包括员工级别信息）
     * @param input
     */
    void createOrUpdateUserExistByUserName(CreateOrUpdateUserInfoInput input);

    /**
     * 创建外单位人员
     */
    Long createExternalUser(CreateExternalUserInput input);

    /**
     * 更新外单位账号信息
     */
    Long updateExternalUserInfo(ExternalUserSettingDto dto);


    /**
     * 删除用户
     *
     * @param entityDto 用户id
     */
    void deleteUser(EntityDto entityDto);

    /**
     * 批量删除用户
     *
     * @param entityDtos 用户id
     */
    void deleteUsers(List<EntityDto> entityDtos);

    /**
     * 解锁用户
     *
     * @param entityDto 用户id
     */
    void unlockUser(EntityDto entityDto);

    /**
     * 修改密码
     *
     * @param cpi 新旧密码
     */
    void changePassword(ChangePasswordInput cpi, UserContext userContext);

    /**
     * 列表修改密码
     *
     * @param input
     */
    void listChangePassword(ChangePwdInput input);

    /**
     * 获取用户用于登陆
     *
     * @param getUsersForLoginInput 输入
     */
    List<UserListForLoginDto> getUsersForLogin(GetUsersForLoginInput getUsersForLoginInput);

    /**
     * 获取登录用户的角色
     *
     * @param userId 用户id
     * @return List<String>
     */
    List<Long> getCurrentRoles(Long userId);

    /**
     * 用户激活
     *
     * @param userId 用户id
     * @param key    验证key
     * @param code   验证码
     */
    void activeUser(Long userId, String key, String code);

    /**
     * 设置用户默认厂站
     *
     * @param setDefaultStationInput 用户厂站
     */
    void setDefaultStation(SetDefaultStationInput setDefaultStationInput);


    /**
     * 检查用户是否可用
     *
     * @param user
     */
    void checkUserValid(User user);

    void saveUserPicture(Long userId, Long minioId, String type);

    /**
     * 忘记密码通过手机号获取重置密码验证码
     *
     * @param userName
     * @param mobile
     */
    void getForgetPwdCaptchaByMobile(String userName, String mobile);

    /**
     * 忘记密码通过邮箱获取重置密码验证码
     *
     * @param userName
     * @param email
     */
    void getForgetPwdCaptchaByEmail(String userName, String email);

    /**
     * 通过验证码重置密码
     *
     * @param userName
     * @param pwd
     * @param captcha
     */
    void resetPwdWithCaptcha(String userName, String pwd, String captcha);

    /**
     * 宿主修改租户管理员密码
     *
     * @param tenantId
     * @param newPwd
     */
    void changeAdminPasswordByHost(Long tenantId, String newPwd);


    /**
     * 根据用户id生成token
     *
     * @param userId
     * @return
     */
    String generateToken(Long userId);


    /**
     * 根据用户名生成登录token
     *
     * @param userName
     */
    String generateTokenByUserName(String userName);

    /**
     * 修改个人信息
     *
     * @param input
     */
    void updateUserInfoBySelf(UpdateUserInfo input);


    /**
     * 检查用户是否是激活状态
     *
     * @param user
     * @return
     */
    boolean checkUserIsActive(User user);

    /**
     * 删除用户
     * @param ids
     */
    void deleteUserByIds(List<Long> ids);
    /**
     * 根据用户名查找用户
     * @param userNames
     */
    List<User> findByUserNames(List<String> userNames);

    void updateUserRoles(Long userId, List<Role> roles);

    String generateExpireTokenByUserName(String userName, long expireTime, TimeUnit unit);

    /**
     * 更新当前用户的信息
     * @param dto
     */
    void updateInfoBySelf(UserInfoDto dto);

    /**
     * 修改用户状态
     */
    void changeStatus(ChangeStatusInput input);
}
