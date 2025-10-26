package com.dusk.module.auth.controller;

import com.dusk.common.core.annotation.Authorize;
import com.dusk.common.core.controller.CruxBaseController;
import com.dusk.common.core.dto.EntityDto;
import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.model.UserContext;
import com.dusk.module.auth.authorization.ExternalManagerAuthProvider;
import com.dusk.module.auth.dto.user.*;
import com.dusk.module.auth.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * @author kefuming
 * @CreateTime 2022-10-28
 */
@RestController
@RequestMapping("externalUser")
@Tag(name = "外单位用户", description = "externalUser")
@Authorize(ExternalManagerAuthProvider.PAGES_EXTERNAL_USERS)
public class ExternalUserController extends CruxBaseController {
    @Resource
    private IUserService userService;

    @Operation(summary = "获取用户列表")
    @RequestMapping(value = "/getUsers", method = RequestMethod.GET)
    public PagedResultDto<UserListDto> getUsers(GetUsersInput getUsersInput) {
        return userService.getUsersList(getUsersInput);
    }

    @Operation(summary = "创建外部用户（不可修改用户名）")
    @PostMapping("/createExternalUserExistByUserName")
    @Authorize(ExternalManagerAuthProvider.PAGES_EXTERNAL_USERS_CREATE)
    public Long createExternalUserExistByUserName(@Valid @RequestBody CreateExternalUserInput input) {
        return userService.createExternalUser(input);
    }

    @Operation(summary = "更新账号信息")
    @PostMapping("/updateExternalUserInfo")
    @Authorize(ExternalManagerAuthProvider.PAGES_EXTERNAL_USERS_EDIT)
    public Long updateExternalUserInfo(@Valid @RequestBody ExternalUserSettingDto input) {
        return userService.updateExternalUserInfo(input);
    }

    @Operation(summary = "获取外单位用户信息用于编辑")
    @GetMapping("/getExternalUserEditInfo/{userId}")
    public GetExternalUserEditOutput getExternalUserEditInfo(@PathVariable Long userId) {
        return userService.getExternalUserEditInfo(userId);
    }

    @Operation(summary = "更新用户个人信息")
    @PostMapping("/updatePersonalInfo")
    @Authorize(ExternalManagerAuthProvider.PAGES_EXTERNAL_USERS_EDIT)
    public void updatePersonalInfo(@Valid @RequestBody PersonalInfoInput infoInput) {
        userService.updatePersonalInfo(infoInput);
    }

    @Operation(summary = "删除用户")
    @DeleteMapping(value = "/deleteUser")
    @Authorize(ExternalManagerAuthProvider.PAGES_EXTERNAL_USERS_DELETE)
    public void deleteUser(@RequestBody EntityDto entityDto) {
        UserContext userContext = getCurrentUser();
        if (userContext.getId().equals(entityDto.getId())) {
            throw new BusinessException("不能删除自己！");
        }
        userService.deleteUser(entityDto);
    }
}
