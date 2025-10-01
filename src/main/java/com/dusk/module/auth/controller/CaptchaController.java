package com.dusk.module.auth.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import com.dusk.common.core.annotation.AllowAnonymous;
import com.dusk.common.core.controller.CruxBaseController;
import com.dusk.module.auth.dto.captcha.CaptchaOutDto;
import com.dusk.module.auth.service.ICaptchaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author kefuming
 * @date 2020-12-15 15:00
 */
@RestController
@RequestMapping("/captcha")
@Api(tags = "Captcha", description = "验证码")
public class CaptchaController extends CruxBaseController {
    @Autowired
    ICaptchaService captchaService;

    @GetMapping
    @AllowAnonymous
    @ApiOperation("获取一个验证码")
    public CaptchaOutDto captcha() {
        return captchaService.getCaptcha();
    }
}
