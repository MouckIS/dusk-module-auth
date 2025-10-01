package com.dusk.module.auth.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import com.dusk.common.core.annotation.AllowAnonymous;
import com.dusk.common.core.controller.CruxBaseController;
import com.dusk.module.auth.dto.mobilelogin.MobileUserDto;
import com.dusk.module.auth.dto.mobilelogin.SendCaptchaInput;
import com.dusk.module.auth.service.IMobileLoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

/**
 * @author pengmengjiang
 * @date 2020/10/14 10:55
 */
@RestController
@RequestMapping("mobile")
@Api(description = "手机登录", tags = "AuthMobile")
public class MobileLoginController extends CruxBaseController {

    @Autowired
    IMobileLoginService mobileLoginService;

    @PostMapping("/captcha")
    @AllowAnonymous
    @ApiOperation("获取手机登录验证码")
    public void captcha(@Valid @RequestBody SendCaptchaInput input, HttpServletRequest request) {
        mobileLoginService.captcha(input, request);
    }

    @PostMapping("/login")
    @AllowAnonymous
    @ApiOperation("手机登录")
    public List<MobileUserDto> login(@RequestParam @ApiParam(value = "手机号", required = true) String mobile,
                                     @RequestParam @ApiParam(value = "验证码", required = true) String captcha) {
        return mobileLoginService.login(mobile, captcha);
    }
}
