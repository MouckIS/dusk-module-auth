package com.dusk.module.ddm.module.auth.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import com.dusk.common.core.annotation.AllowAnonymous;
import com.dusk.common.core.annotation.Authorize;
import com.dusk.common.core.annotation.IgnoreResponseAdvice;
import com.dusk.common.core.auth.authentication.AuthorizationContextHolder;
import com.dusk.common.core.controller.CruxBaseController;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.model.UserContext;
import com.dusk.common.core.response.BaseApiResult;
import com.dusk.common.core.utils.SecurityUtils;
import com.dusk.module.auth.authorization.TokenConfigProvider;
import com.dusk.module.auth.common.manage.TokenAuthManager;
import com.dusk.module.auth.common.model.LoginRequest;
import com.dusk.module.auth.dto.token.TokenSign;
import com.dusk.module.auth.igw.IAppSSOLoginService;
import com.dusk.module.auth.service.ICaptchaService;
import com.dusk.module.auth.service.IScanCodeLoginService;
import com.dusk.module.auth.service.ISsoTokenService;
import com.dusk.module.auth.service.ITokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * @author kefuming
 * @date 2020-05-28 16:55
 */
@RestController
@RequestMapping("token-auth")
@Api(tags = "TokenAuth", description = "登陆相关", position = -999)
public class TokenAuthController extends CruxBaseController {
    @Autowired
    private TokenAuthManager tokenAuthManager;
    @Autowired
    ICaptchaService captchaService;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    IScanCodeLoginService scanCodeLoginService;
    @Autowired
    SecurityUtils securityUtils;
    @Autowired(required = false)
    IAppSSOLoginService appSSOLoginService;
    @Autowired
    ISsoTokenService ssoTokenService;
    @Autowired
    ITokenService tokenService;

    @AllowAnonymous
    @RequestMapping(value = "authenticate", method = RequestMethod.POST)
    @ApiOperation("测试用的登陆接口，前端不要用来做登陆")
    public String authenticate(@Valid @RequestBody LoginRequest input, HttpServletRequest request) {
        boolean verifyCaptcha = captchaService.verifyCaptcha(input, request);
        if (!verifyCaptcha) {
            throw new BadCredentialsException("验证码错误");
        }
        //处理用户登录验证的逻辑
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(input.getUsername(), input.getPassword());
        Authentication authResult = authenticationManager.authenticate(token);
        if (authResult.isAuthenticated()) {
            UserContext userContext = (UserContext) authResult.getPrincipal();
            return tokenAuthManager.generateToken(userContext);
        } else {
            throw new BusinessException("帐户名或者密码错误");
        }
    }

    @AllowAnonymous
    @GetMapping(value = "scan-code/key")
    @ApiOperation("获取扫码登陆的唯一的key")
    public String getScanCodeKey() {
        return scanCodeLoginService.getLoginKey();
    }

    @AllowAnonymous
    @GetMapping(value = "scan-code/token/{key}")
    @IgnoreResponseAdvice
    @ApiOperation("获取扫码登陆的key对应的token，可能不存在key的情况 code为1001")
    public BaseApiResult<String> getScanCodeToken(@PathVariable String key) {
        return scanCodeLoginService.getToken(key);
    }

    @GetMapping(value = "scan-code/login/{key}")
    @IgnoreResponseAdvice
    @ApiOperation("对指定的key执行扫码登陆，目前是1年的token")
    public void scanCodeLogin(@PathVariable String key) {
        scanCodeLoginService.login(key);
    }

    @ApiOperation("登出接口")
    @RequestMapping(value = "logout", method = RequestMethod.POST)
    public void logout() {
        tokenAuthManager.removeToken(AuthorizationContextHolder.getAuthorization());
    }


    @ApiOperation("刷新token")
    @RequestMapping(value = "refresh", method = RequestMethod.GET)
    public String refresh() {
        //TODO:是否需要注销掉之前的token？
        return tokenAuthManager.generateToken(getCurrentUser());
    }


    @AllowAnonymous
    @GetMapping("igw-login")
    @ApiOperation("获取i国网 token")
    public String igwLogin(String ticket) {
        if (appSSOLoginService != null) {
            return appSSOLoginService.login(ticket);
        }
        throw new BusinessException("禁止访问");

    }

    @AllowAnonymous
    @GetMapping("ssoSm4Token")
    @ApiOperation("基于国密4加密机制给第三方快速签发token")
    public String ssoSm4Token(String encryptStr) {
        return ssoTokenService.ssoSm4Token(encryptStr);
    }

    @authorize(TokenConfigProvider.PAGES_FOREVER_TOKEN_SIGN)
    @PostMapping("foreverTokenSign")
    @ApiOperation("长久token签发")
    public String foreverTokenSign(@Valid @RequestBody TokenSign tokenSign) {
        return tokenService.foreverTokenSign(tokenSign);
    }
}
