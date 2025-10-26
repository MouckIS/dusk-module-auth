package com.dusk.module.auth.controller;

import com.dusk.common.core.annotation.AllowAnonymous;
import com.dusk.common.core.controller.CruxBaseController;
import com.dusk.module.auth.dto.mobilelogin.MobileUserDto;
import com.dusk.module.auth.dto.mobilelogin.SendCaptchaInput;
import com.dusk.module.auth.service.IMobileLoginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author pengmengjiang
 * @date 2020/10/14 10:55
 */
@RestController
@RequestMapping("mobile")
@Tag(name = "手机登录", description = "AuthMobile")
public class MobileLoginController extends CruxBaseController {

    @Resource
    IMobileLoginService mobileLoginService;

    @PostMapping("/captcha")
    @AllowAnonymous
    @Operation(summary = "获取手机登录验证码")
    public void captcha(@Valid @RequestBody SendCaptchaInput input, HttpServletRequest request) {
        mobileLoginService.captcha(input, request);
    }

    @PostMapping("/login")
    @AllowAnonymous
    @Operation(summary = "手机登录")
    public List<MobileUserDto> login(@RequestParam @Parameter(description = "手机号", required = true) String mobile,
                                     @RequestParam @Parameter(description = "验证码", required = true) String captcha) {
        return mobileLoginService.login(mobile, captcha);
    }
}
