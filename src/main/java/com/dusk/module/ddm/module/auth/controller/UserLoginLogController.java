package com.dusk.module.ddm.module.auth.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import com.dusk.common.core.annotation.Authorize;
import com.dusk.common.core.controller.CruxBaseController;
import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.module.auth.authorization.UserLoginLogAuthProvider;
import com.dusk.module.auth.dto.loginlog.ListUserLoginLogInput;
import com.dusk.module.auth.dto.loginlog.UserLoginLogDto;
import com.dusk.module.auth.service.IUserLoginLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author kefuming
 * @description: TODO
 * @date 2022/10/28
 */
@RestController
@RequestMapping("userLoginLog")
@Api(description = "用户日志管理", tags = "UserLoginLog")
@authorize(UserLoginLogAuthProvider.PAGES_USER_LOGIN_LOG)
public class UserLoginLogController extends CruxBaseController {
    @Autowired
    private IUserLoginLogService userLoginLogService;

    @GetMapping("listLog")
    @ApiOperation(value = "查询用户日志")
    @authorize(UserLoginLogAuthProvider.PAGES_USER_LOGIN_LOG)
    public PagedResultDto<UserLoginLogDto> listLog(ListUserLoginLogInput input) {
        return userLoginLogService.listLog(input);
    }
}
