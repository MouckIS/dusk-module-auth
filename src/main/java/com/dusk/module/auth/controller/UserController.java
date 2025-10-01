package com.dusk.module.auth.controller;

import com.dusk.commom.rpc.auth.dto.ChangePwdInput;
import com.dusk.module.auth.dto.user.*;
import com.dusk.module.auth.service.IFeatureChecker;
import com.github.dozermapper.core.Mapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import com.dusk.common.core.annotation.AllowAnonymous;
import com.dusk.common.core.annotation.Authorize;
import com.dusk.common.core.auth.authentication.LoginUserIdContextHolder;
import com.dusk.common.core.controller.CruxBaseController;
import com.dusk.common.core.dto.EntityDto;
import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.model.UserContext;
import com.dusk.common.core.utils.DozerUtils;
import com.dusk.module.auth.authorization.AdminUserAuthProvider;
import com.dusk.module.auth.dto.user.*;
import com.dusk.module.auth.entity.User;
import com.dusk.module.auth.feature.UserFeatureProvider;
import com.dusk.module.auth.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.List;

//TODO:添加登陆用户得更改个人信息接口 常见于更新名字，邮箱，手机号等

/**
 * @author kefuming
 * @date 2020/5/12 17:18
 */
@RestController
@RequestMapping("user")
@Api(description = "用户", tags = "User")
@Authorize(AdminUserAuthProvider.PAGES_ADMINISTRATION_USERS)
public class UserController extends CruxBaseController {
    @Autowired
    IUserService userService;
    @Autowired
    private Mapper dozerMapper;
    @Autowired
    IFeatureChecker featureChecker;


    @ApiOperation(value = "获取用户列表")
    @RequestMapping(value = "/getUsers", method = RequestMethod.GET)
    public PagedResultDto<UserListDto> getUsers(GetUsersInput getUsersInput) {
        return userService.getUsersList(getUsersInput);
    }

    @ApiOperation(value = "获取当前厂站的用户列表")
    @RequestMapping(value = "/getOrgaUsers", method = RequestMethod.GET)
    public PagedResultDto<UserListDto> getOrgaUsers(@Valid GetOrgaUsersInput getOrgaUsersInput) {
        Page<User> page = userService.getOrgaUsers(getOrgaUsersInput);
        return DozerUtils.mapToPagedResultDto(dozerMapper, page, UserListDto.class);
    }

    @ApiOperation(value = "通过角色Code列表获取所有用户")
    @RequestMapping(value = "/getUsersByRoleCodes", method = RequestMethod.GET)
    public PagedResultDto<UserListDto> getUsersByRoleCodes(GetUsersByRoleCodesInput getUsersInput) {
        Page<User> page = userService.getUsersByRoleCodes(getUsersInput);
        return DozerUtils.mapToPagedResultDto(dozerMapper, page, UserListDto.class);
    }

    @ApiOperation(value = "通过角色名称列表获取所有用户")
    @RequestMapping(value = "/getUsersByRoleNames", method = RequestMethod.GET)
    public PagedResultDto<UserForSelectDto> getUsersByRoleNames(@Valid GetUsersByRoleNameInput input) {
        Page<User> page = userService.getUsersByRoleName(input);
        return DozerUtils.mapToPagedResultDto(dozerMapper, page, UserForSelectDto.class);
    }

    @ApiOperation(value = "获取所有用户列表（不分页）")
    @RequestMapping(value = "/getAllUsers", method = RequestMethod.GET)
    public List<UserListDto> getAllUsers() {
        List<User> userList = userService.getAllUsers();
        return DozerUtils.mapList(dozerMapper, userList, UserListDto.class);
    }

    @ApiOperation(value = "导出用户到excel")
    @RequestMapping(value = "/getUsersToExcel", method = RequestMethod.GET)
    @Authorize(AdminUserAuthProvider.PAGES_ADMINISTRATION_USERS)
    public void getUsersToExcel(HttpServletResponse response) {
        userService.getUsersToExcel(response);
    }

    @ApiOperation(value = "获取当前用户详情用于编辑")
    @RequestMapping(value = "/getUsersForEdit", method = RequestMethod.GET)
    @Authorize(AdminUserAuthProvider.PAGES_ADMINISTRATION_USERS)
    public GetUserForEditOutput getUsersForEdit(EntityDto entityDto) {
        return userService.getUserForEdit(entityDto);
    }

    @ApiOperation(value = "获取当前用户详情")
    @RequestMapping(value = "/getUserInfo", method = RequestMethod.GET)
    public GetUserForEditOutput getUserInfo() {
        return userService.getUserForEdit(new EntityDto(LoginUserIdContextHolder.getUserId()));
    }

    @ApiOperation(value = "获取当前用户个人信息")
    @GetMapping(value = "/getInfo/{id}")
    public GetUserInfoOutput getPersonalInfo(@PathVariable Long id) {
        return userService.getUserInfo(id);
    }

    @ApiOperation(value = "更新用户个人信息")
    @PostMapping("/updatePersonalInfo")
    @Authorize(AdminUserAuthProvider.PAGES_ADMINISTRATION_USERS_EDIT)
    public void updatePersonalInfo(@RequestBody @Valid PersonalInfoInput infoInput) {
        userService.updatePersonalInfo(infoInput);
    }

    @ApiOperation(value = "更新当前用户的个人信息")
    @PostMapping("/updateInfoBySelf")
    public void updateInfo(@RequestBody @Valid UserInfoDto dto) {
        userService.updateInfoBySelf(dto);
    }

    @ApiOperation(value = "创建/修改用户（可修改用户名）")
    @RequestMapping(value = "/createOrUpdateUser", method = RequestMethod.POST)
    @Authorize(AdminUserAuthProvider.PAGES_ADMINISTRATION_USERS_CREATE)
    //TODO:暂不删除，未来可用于列表编辑
    public void createOrUpdateUser(@RequestBody @Valid CreateUserInput createUserInput) {
        userService.createOrUpdateUser(createUserInput);
    }

    @ApiOperation(value = "创建/修改用户（不可修改用户名）")
    @PostMapping(value = "/createOrUpdateUserExistByUserName")
    @Authorize(AdminUserAuthProvider.PAGES_ADMINISTRATION_USERS_CREATE)
    public void createOrUpdateUserExistByUserName(@RequestBody CreateOrUpdateUserInfoInput createOrUpdateUserInput) {
        userService.createOrUpdateUserExistByUserName(createOrUpdateUserInput);
    }

    @ApiOperation(value = "删除用户")
    @DeleteMapping(value = "/deleteUser")
    @Authorize(AdminUserAuthProvider.PAGES_ADMINISTRATION_USERS_DELETE)
    public void deleteUser(@RequestBody EntityDto entityDto) {
        UserContext userContext = getCurrentUser();
        if (userContext.getId().equals(entityDto.getId())) {
            throw new BusinessException("不能删除自己！");
        }
        userService.deleteUser(entityDto);
    }

    @ApiOperation(value = "批量删除用户")
    @DeleteMapping(value = "/deleteUsers")
    @Authorize(AdminUserAuthProvider.PAGES_ADMINISTRATION_USERS_DELETE)
    public void deleteUsers(@RequestBody List<EntityDto> entityDtos) {
        userService.deleteUsers(entityDtos);
    }

    @ApiOperation(value = "解锁用户")
    @Authorize(AdminUserAuthProvider.PAGES_ADMINISTRATION_USERS_EDIT)
    @RequestMapping(value = "/unlockUser", method = RequestMethod.POST)
    public void unlockUser(@RequestBody EntityDto entityDto) {
        userService.unlockUser(entityDto);
    }

    //登陆可访问
    @ApiOperation(value = "修改密码")
    @RequestMapping(value = "/changePassword", method = RequestMethod.POST)
    public void changePassword(@RequestBody ChangePasswordInput cpi) {
        userService.changePassword(cpi, getCurrentUser());
    }

    @ApiOperation(value = "列表修改密码")
    @RequestMapping(value = "/list/change-pwd", method = RequestMethod.POST)
    @Authorize(AdminUserAuthProvider.PAGES_ADMINISTRATION_USERS_CREATE)
    public void listChangePwd(@RequestBody ChangePwdInput input) {
        userService.listChangePassword(input);
    }

    @ApiOperation(value = "修改用户状态")
    @PostMapping("/changeUserStatus")
    @Authorize(AdminUserAuthProvider.PAGES_ADMINISTRATION_USERS_CHANGEPERMISSIONS)
    public void changeUserStatus(@Valid @RequestBody ChangeStatusInput input) {
        userService.changeStatus(input);
    }

    @ApiOperation(value = "获取用户名称列表用于登陆")
    @RequestMapping(value = "/getUsersForLogin", method = RequestMethod.GET)
    @AllowAnonymous
    public List<UserListForLoginDto> getUsersForLogin(GetUsersForLoginInput getUsersForLoginInput) {
        if (!featureChecker.isEnabled(UserFeatureProvider.APP_USER_ALLOW_GET_USERS_FOR_LOGIN_BY_ANONYMOUS) && getCurrentUser() == null) {//特性中配置不允许匿名访问
            throw new BusinessException("禁止访问");
        }
        return userService.getUsersForLogin(getUsersForLoginInput);
    }

    @ApiOperation(value = "用户激活，邮件链接方式")
    @AllowAnonymous
    @RequestMapping(value = "/activeUser", method = RequestMethod.GET)
    public void activeUser(Long userId, String key, String code) {
        userService.activeUser(userId, key, code);
    }

    @ApiOperation(value = "获取登录用户的角色")
    @RequestMapping(value = "/getCurrentRoles", method = RequestMethod.GET)
    //TODO:优化，获取当前登陆用户得角色列表dto信息
    public List<Long> getCurrentRoles() {
        return userService.getCurrentRoles(getCurrentUser().getId());
    }

    @ApiOperation(value = "设置默认厂站")
    @RequestMapping(value = "/setDefaultStation", method = RequestMethod.POST)
    public void setDefaultStation(@RequestBody @Valid SetDefaultStationInput setDefaultStationInput) {
        userService.setDefaultStation(setDefaultStationInput);
    }


    @ApiOperation(value = "用户上传（头像、签字）图片")
    @PostMapping(value = "saveMyselfPicture")
    public void saveMyselfPicture(@RequestParam("minioId") Long minioId, @ApiParam(name = "type", value = "signature或者profile", required = true) @RequestParam("type") String type) {
        userService.saveUserPicture(getCurrentUser().getId(), minioId, type);
    }

    @ApiOperation(value = "管理员上传（头像、签字）图片")
    @PostMapping(value = "saveUserPicture")
    @Authorize(AdminUserAuthProvider.PAGES_ADMINISTRATION_USERS_EDIT)
    public void saveUserPicture(@RequestParam("userId") Long userId, @RequestParam("minioId") Long minioId, @ApiParam(name = "type", value = "signature或者profile", required = true) @RequestParam("type") String type) {
        userService.saveUserPicture(userId, minioId, type);
    }

    @GetMapping("pwd/forget/mobile/captcha")
    @ApiOperation("忘记密码通过手机号获取重置密码验证码")
    @AllowAnonymous
    public void getForgetPwdCaptchaByMobile(@ApiParam(required = true, value = "用户名") @RequestParam @NotBlank(message = "用户名不能为空") String userName,
                                            @ApiParam(required = true, value = "手机号") @RequestParam @NotBlank(message = "手机号不能为空") String mobile) {
        userService.getForgetPwdCaptchaByMobile(userName, mobile);
    }

    @GetMapping("pwd/forget/email/captcha")
    @ApiOperation("忘记密码通过邮箱获取重置密码验证码")
    @AllowAnonymous
    public void getForgetPwdCaptchaByEmail(@ApiParam(required = true, value = "用户名") @RequestParam @NotBlank(message = "用户名不能为空") String userName,
                                           @ApiParam(required = true, value = "邮箱") @RequestParam @NotBlank(message = "邮箱不能为空") String email) {
        userService.getForgetPwdCaptchaByEmail(userName, email);
    }

    @PostMapping("pwd/reset/captcha")
    @ApiOperation("通过验证码重置密码")
    @AllowAnonymous
    public void resetPwdWithCaptcha(@ApiParam(required = true, value = "用户名") @RequestParam @NotBlank(message = "用户名不能为空") String userName,
                                    @ApiParam(required = true, value = "新密码") @RequestParam @NotBlank(message = "密码不能为空") String pwd,
                                    @ApiParam(required = true, value = "验证码") @RequestParam @NotBlank(message = "验证码不能为空") String captcha) {
        userService.resetPwdWithCaptcha(userName, pwd, captcha);
    }


    @PostMapping("/updateUserInfoBySelf")
    @ApiOperation("更新个人信息")
    public void updateUserInfoBySelf(@Valid @RequestBody UpdateUserInfo input) {
        userService.updateUserInfoBySelf(input);
    }
}
