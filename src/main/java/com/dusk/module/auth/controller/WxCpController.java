package com.dusk.module.auth.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import com.dusk.common.framework.annotation.AllowAnonymous;
import com.dusk.common.framework.annotation.IgnoreResponseAdvice;
import com.dusk.common.framework.controller.CruxBaseController;
import com.dusk.module.auth.dto.weixin.WxCpUserAuthorizationResult;
import com.dusk.module.auth.service.IWxCpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

/**
 * @author jianjianhong
 * @date 2023-10-11 11:14
 */
@RestController
@Api(tags = "WxCp", description = "企业微信")
@RequestMapping("/wxcp")
@Slf4j
public class WxCpController extends CruxBaseController {
    @Autowired
    private IWxCpService service;

    @GetMapping("/serviceProviderUrl")
    @ApiOperation("企业微信服务商应用验证URL")
    @AllowAnonymous
    @IgnoreResponseAdvice
    public String serviceProviderUrlGet(@RequestParam String msg_signature, @RequestParam Integer timestamp, @RequestParam String nonce, @RequestParam String echostr) {
        return service.serviceProviderUrlGet(msg_signature, timestamp, nonce, echostr);
    }

    @PostMapping("/serviceProviderUrl")
    @ApiOperation("企业微信服务商授权通知")
    @AllowAnonymous
    public String serviceProviderUrlPost(String msg_signature, String timestamp, String nonce, @RequestBody(required = false) String body) {
        log.info("授权成功通知: {}", body);
        service.serviceProviderUrlPost(msg_signature, timestamp, nonce, body);
        return "";
    }


    @GetMapping("/bind")
    @ApiOperation("绑定企业微信用户")
    @AllowAnonymous
    public WxCpUserAuthorizationResult bind(@RequestParam String wxUserId, @RequestParam String userName, @RequestParam String password) {
        return service.bind(wxUserId, userName, password);
    }

    @GetMapping("/authorization")
    @ApiOperation("企业微信用户身份验证")
    @AllowAnonymous
    public RedirectView authorization(@RequestParam String code, @RequestParam(required = false) String state, @RequestParam String n, @RequestParam String r) {
        WxCpUserAuthorizationResult result = service.authorization(code, state, n);
        String redirectUrl = String.format("%s?token=%s&userId=%s", r, result.getToken(), result.getWxUserId());
        //return "redirect:" + redirectUrl;
        log.info(redirectUrl);
        return new RedirectView(redirectUrl);
    }

  /*  @PostMapping("/sendCardMessage")
    @ApiOperation("发送卡片消息")
    @AllowAnonymous
    public void sendCardMessage(@Valid @RequestBody WxCpTextCardMessage input) {
        service.sendCardMessage(input);
    }*/
}
