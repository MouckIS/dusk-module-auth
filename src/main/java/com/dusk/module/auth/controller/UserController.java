package com.dusk.module.auth.controller;

import com.dusk.common.core.annotation.AllowAnonymous;
import com.dusk.common.core.annotation.Authorize;
import com.dusk.common.core.auth.authentication.LoginUserIdContextHolder;
import com.dusk.common.core.controller.CruxBaseController;
import com.dusk.common.core.dto.EntityDto;
import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.model.UserContext;
import com.dusk.common.core.utils.MapperUtil;
import com.dusk.common.rpc.auth.dto.ChangePwdInput;
import com.dusk.module.auth.authorization.AdminUserAuthProvider;
import com.dusk.module.auth.dto.user.*;
import com.dusk.module.auth.entity.User;
import com.dusk.module.auth.feature.UserFeatureProvider;
import com.dusk.module.auth.mapper.UserMapper;
import com.dusk.module.auth.service.IFeatureChecker;
import com.dusk.module.auth.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

//TODO:添加登陆用户得更改个人信息接口 常见于更新名字，邮箱，手机号等

/**
 * @author kefuming
 * @date 2020/5/12 17:18
 */
@RestController
@RequestMapping("user")
@Tag(name = "用户", description = "User")
@Authorize(AdminUserAuthProvider.PAGES_ADMINISTRATION_USERS)
public class UserController extends CruxBaseController {
    @Resource
    private IUserService userService;
    @Resource
    private IFeatureChecker featureChecker;

    private final UserMapper mapper = UserMapper.INSTANCE;


    @Operation(summary = "获取用户列表")
    @RequestMapping(value = "/getUsers", method = RequestMethod.GET)
    public PagedResultDto<UserListDto> getUsers(GetUsersInput getUsersInput) {
        return userService.getUsersList(getUsersInput);
    }

    @Operation(summary = "获取当前厂站的用户列表")
    @RequestMapping(value = "/getOrgaUsers", method = RequestMethod.GET)
    public PagedResultDto<UserListDto> getOrgaUsers(@Valid GetOrgaUsersInput getOrgaUsersInput) {
        Page<User> page = userService.getOrgaUsers(getOrgaUsersInput);
        return MapperUtil.mapToPagedResultDto(page, mapper::toListDto);
    }

    @Operation(summary = "通过角色Code列表获取所有用户")
    @RequestMapping(value = "/getUsersByRoleCodes", method = RequestMethod.GET)
    public PagedResultDto<UserListDto> getUsersByRoleCodes(GetUsersByRoleCodesInput getUsersInput) {
        Page<User> page = userService.getUsersByRoleCodes(getUsersInput);
        return MapperUtil.mapToPagedResultDto(page, mapper::toListDto);
    }

    @Operation(summary = "通过角色名称列表获取所有用户")
    @RequestMapping(value = "/getUsersByRoleNames", method = RequestMethod.GET)
    public PagedResultDto<UserForSelectDto> getUsersByRoleNames(@Valid GetUsersByRoleNameInput input) {
        Page<User> page = userService.getUsersByRoleName(input);
        return MapperUtil.mapToPagedResultDto(page, mapper::toSelectDto);
    }

    @Operation(summary = "获取所有用户列表（不分页）")
    @RequestMapping(value = "/getAllUsers", method = RequestMethod.GET)
    public List<UserListDto> getAllUsers() {
        List<User> userList = userService.getAllUsers();
        return MapperUtil.mapList(userList, mapper::toListDto);
    }

    @Operation(summary = "导出用户到excel")
    @RequestMapping(value = "/getUsersToExcel", method = RequestMethod.GET)
    @Authorize(AdminUserAuthProvider.PAGES_ADMINISTRATION_USERS)
    public void getUsersToExcel(HttpServletResponse response) {
        userService.getUsersToExcel(response);
    }

    @Operation(summary = "获取当前用户详情用于编辑")
    @RequestMapping(value = "/getUsersForEdit", method = RequestMethod.GET)
    @Authorize(AdminUserAuthProvider.PAGES_ADMINISTRATION_USERS)
    public GetUserForEditOutput getUsersForEdit(EntityDto entityDto) {
        return userService.getUserForEdit(entityDto);
    }

    @Operation(summary = "获取当前用户详情")
    @RequestMapping(value = "/getUserInfo", method = RequestMethod.GET)
    public GetUserForEditOutput getUserInfo() {
        return userService.getUserForEdit(new EntityDto(LoginUserIdContextHolder.getUserId()));
    }

    @Operation(summary = "获取当前用户个人信息")
    @GetMapping(value = "/getInfo/{id}")
    public GetUserInfoOutput getPersonalInfo(@PathVariable Long id) {
        return userService.getUserInfo(id);
    }

    @Operation(summary = "更新用户个人信息")
    @PostMapping("/updatePersonalInfo")
    @Authorize(AdminUserAuthProvider.PAGES_ADMINISTRATION_USERS_EDIT)
    public void updatePersonalInfo(@RequestBody @Valid PersonalInfoInput infoInput) {
        userService.updatePersonalInfo(infoInput);
    }

    @Operation(summary = "更新当前用户的个人信息")
    @PostMapping("/updateInfoBySelf")
    public void updateInfo(@RequestBody @Valid UserInfoDto dto) {
        userService.updateInfoBySelf(dto);
    }

    @Operation(summary = "创建/修改用户（可修改用户名）")
    @RequestMapping(value = "/createOrUpdateUser", method = RequestMethod.POST)
    @Authorize(AdminUserAuthProvider.PAGES_ADMINISTRATION_USERS_CREATE)
    //TODO:暂不删除，未来可用于列表编辑
    public void createOrUpdateUser(@RequestBody @Valid CreateUserInput createUserInput) {
        userService.createOrUpdateUser(createUserInput);
    }

    @Operation(summary = "创建/修改用户（不可修改用户名）")
    @PostMapping(value = "/createOrUpdateUserExistByUserName")
    @Authorize(AdminUserAuthProvider.PAGES_ADMINISTRATION_USERS_CREATE)
    public void createOrUpdateUserExistByUserName(@RequestBody CreateOrUpdateUserInfoInput createOrUpdateUserInput) {
        userService.createOrUpdateUserExistByUserName(createOrUpdateUserInput);
    }

    @Operation(summary = "删除用户")
    @DeleteMapping(value = "/deleteUser")
    @Authorize(AdminUserAuthProvider.PAGES_ADMINISTRATION_USERS_DELETE)
    public void deleteUser(@RequestBody EntityDto entityDto) {
        UserContext userContext = getCurrentUser();
        if (userContext.getId().equals(entityDto.getId())) {
            throw new BusinessException("不能删除自己！");
        }
        userService.deleteUser(entityDto);
    }

    @Operation(summary = "批量删除用户")
    @DeleteMapping(value = "/deleteUsers")
    @Authorize(AdminUserAuthProvider.PAGES_ADMINISTRATION_USERS_DELETE)
    public void deleteUsers(@RequestBody List<EntityDto> entityDtos) {
        userService.deleteUsers(entityDtos);
    }

    @Operation(summary = "解锁用户")
    @Authorize(AdminUserAuthProvider.PAGES_ADMINISTRATION_USERS_EDIT)
    @RequestMapping(value = "/unlockUser", method = RequestMethod.POST)
    public void unlockUser(@RequestBody EntityDto entityDto) {
        userService.unlockUser(entityDto);
    }

    //登陆可访问
    @Operation(summary = "修改密码")
    @RequestMapping(value = "/changePassword", method = RequestMethod.POST)
    public void changePassword(@RequestBody ChangePasswordInput cpi) {
        userService.changePassword(cpi, getCurrentUser());
    }

    @Operation(summary = "列表修改密码")
    @RequestMapping(value = "/list/change-pwd", method = RequestMethod.POST)
    @Authorize(AdminUserAuthProvider.PAGES_ADMINISTRATION_USERS_CREATE)
    public void listChangePwd(@RequestBody ChangePwdInput input) {
        userService.listChangePassword(input);
    }

    @Operation(summary = "修改用户状态")
    @PostMapping("/changeUserStatus")
    @Authorize(AdminUserAuthProvider.PAGES_ADMINISTRATION_USERS_CHANGEPERMISSIONS)
    public void changeUserStatus(@Valid @RequestBody ChangeStatusInput input) {
        userService.changeStatus(input);
    }

    @Operation(summary = "获取用户名称列表用于登陆")
    @RequestMapping(value = "/getUsersForLogin", method = RequestMethod.GET)
    @AllowAnonymous
    public List<UserListForLoginDto> getUsersForLogin(GetUsersForLoginInput getUsersForLoginInput) {
        if (!featureChecker.isEnabled(UserFeatureProvider.APP_USER_ALLOW_GET_USERS_FOR_LOGIN_BY_ANONYMOUS) && getCurrentUser() == null) {//特性中配置不允许匿名访问
            throw new BusinessException("禁止访问");
        }
        return userService.getUsersForLogin(getUsersForLoginInput);
    }

    @Operation(summary = "用户激活，邮件链接方式")
    @AllowAnonymous
    @RequestMapping(value = "/activeUser", method = RequestMethod.GET)
    public void activeUser(Long userId, String key, String code) {
        userService.activeUser(userId, key, code);
    }

    @Operation(summary = "获取登录用户的角色")
    @RequestMapping(value = "/getCurrentRoles", method = RequestMethod.GET)
    //TODO:优化，获取当前登陆用户得角色列表dto信息
    public List<Long> getCurrentRoles() {
        return userService.getCurrentRoles(getCurrentUser().getId());
    }

    @Operation(summary = "设置默认厂站")
    @RequestMapping(value = "/setDefaultStation", method = RequestMethod.POST)
    public void setDefaultStation(@RequestBody @Valid SetDefaultStationInput setDefaultStationInput) {
        userService.setDefaultStation(setDefaultStationInput);
    }


    @Operation(summary = "用户上传（头像、签字）图片")
    @PostMapping(value = "saveMyselfPicture")
    public void saveMyselfPicture(@RequestParam("minioId") Long minioId, @Parameter(description = "signature或者profile", name = "type", required = true) @RequestParam("type") String type) {
        userService.saveUserPicture(getCurrentUser().getId(), minioId, type);
    }

    @Operation(summary = "管理员上传（头像、签字）图片")
    @PostMapping(value = "saveUserPicture")
    @Authorize(AdminUserAuthProvider.PAGES_ADMINISTRATION_USERS_EDIT)
    public void saveUserPicture(@RequestParam("userId") Long userId, @RequestParam("minioId") Long minioId, @Parameter(description = "signature或者profile", name = "type", required = true) @RequestParam("type") String type) {
        userService.saveUserPicture(userId, minioId, type);
    }

    @GetMapping("pwd/forget/mobile/captcha")
    @Operation(summary = "忘记密码通过手机号获取重置密码验证码")
    @AllowAnonymous
    public void getForgetPwdCaptchaByMobile(@Parameter(description = "用户名", required = true) @RequestParam @NotBlank(message = "用户名不能为空") String userName,
                                            @Parameter(description = "手机号", required = true) @RequestParam @NotBlank(message = "手机号不能为空") String mobile) {
        userService.getForgetPwdCaptchaByMobile(userName, mobile);
    }

    @GetMapping("pwd/forget/email/captcha")
    @Operation(summary = "忘记密码通过邮箱获取重置密码验证码")
    @AllowAnonymous
    public void getForgetPwdCaptchaByEmail(@Parameter(description = "用户名", required = true) @RequestParam @NotBlank(message = "用户名不能为空") String userName,
                                           @Parameter(description = "邮箱", required = true) @RequestParam @NotBlank(message = "邮箱不能为空") String email) {
        userService.getForgetPwdCaptchaByEmail(userName, email);
    }

    @PostMapping("pwd/reset/captcha")
    @Operation(summary = "通过验证码重置密码")
    @AllowAnonymous
    public void resetPwdWithCaptcha(@Parameter(description = "用户名", required = true) @RequestParam @NotBlank(message = "用户名不能为空") String userName,
                                    @Parameter(description = "新密码", required = true) @RequestParam @NotBlank(message = "密码不能为空") String pwd,
                                    @Parameter(description = "验证码", required = true) @RequestParam @NotBlank(message = "验证码不能为空") String captcha) {
        userService.resetPwdWithCaptcha(userName, pwd, captcha);
    }


    @PostMapping("/updateUserInfoBySelf")
    @Operation(summary = "更新个人信息")
    public void updateUserInfoBySelf(@Valid @RequestBody UpdateUserInfo input) {
        userService.updateUserInfoBySelf(input);
    }
}
