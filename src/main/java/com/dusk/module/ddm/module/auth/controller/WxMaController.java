package com.dusk.module.ddm.module.auth.controller;

import cn.binarywang.wx.miniapp.bean.WxMaPhoneNumberInfo;
import cn.binarywang.wx.miniapp.bean.WxMaUserInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import com.dusk.common.core.annotation.AllowAnonymous;
import com.dusk.common.core.controller.CruxBaseController;
import com.dusk.module.auth.dto.mobilelogin.MobileUserDto;
import com.dusk.module.auth.dto.weixin.WxMaSessionResult;
import com.dusk.module.auth.service.IWxMaService;
import com.dusk.module.auth.service.IWxMaTestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author kefuming
 * @date 2020-12-25 11:14
 */
@RestController
@Api(tags = "WxMa", description = "微信小程序")
@RequestMapping("/wx/user/{appid}")
@Slf4j
public class WxMaController extends CruxBaseController {
    @Autowired
    IWxMaService wxMaService;

    @Autowired(required = false)
    IWxMaTestService wxMaTestService;


    @GetMapping("/session")
    @ApiOperation("获取微信登陆session_key，如果曾经用手机号登陆过，回返回登陆参数")
    @AllowAnonymous
    public WxMaSessionResult getSession(@PathVariable String appid, @RequestParam(required = true) String code) {
        return wxMaService.getSession(appid, code);
    }

    /**
     * <pre>
     * 获取用户信息接口
     * </pre>
     */
    @GetMapping("/info")
    @ApiOperation("获取用户信息接口")
    @AllowAnonymous
    public WxMaUserInfo info(@PathVariable String appid, @RequestParam(required = true) String openid, @RequestParam(required = true) String encryptedData, @RequestParam(required = true) String iv) {
        return wxMaService.info(appid, openid, encryptedData, iv);
    }

    /**
     * <pre>
     * 获取用户手机号信息接口
     * </pre>
     */
    @GetMapping("/phone")
    @ApiOperation("获取用户手机号接口")
    @AllowAnonymous
    public WxMaPhoneNumberInfo phone(@PathVariable String appid, @RequestParam(required = true) String openid, @RequestParam(required = true) String encryptedData, @RequestParam(required = true) String iv) {
        return wxMaService.phone(appid, openid, encryptedData, iv);
    }

    @GetMapping("/login")
    @AllowAnonymous
    @ApiOperation("微信小程序登陆，获取登陆参数")
    public List<MobileUserDto> login(@PathVariable String appid, @RequestParam(required = true) String openid, @RequestParam(required = true) String encryptedData, @RequestParam(required = true) String iv) {
        return wxMaService.login(appid, openid, encryptedData, iv);
    }

}
