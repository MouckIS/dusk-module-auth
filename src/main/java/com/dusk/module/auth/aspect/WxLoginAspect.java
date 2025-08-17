package com.dusk.module.auth.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import com.dusk.module.auth.dto.mobilelogin.MobileUserDto;
import com.dusk.module.auth.dto.weixin.WxMaSessionResult;
import com.dusk.module.auth.entity.UserWxRelation;
import com.dusk.module.auth.service.IUserWxRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author kefuming
 * @date 2021-07-23 15:44
 */
@Aspect
@Component
@Slf4j
public class WxLoginAspect {
    @Autowired
    IUserWxRelationService userWxRelationService;

    @Around("execution(* com.dusk.module.auth.service.IWxMaService.login(..))")
    public Object aroundWxMicroProgramLogin(ProceedingJoinPoint joinPoint) throws Throwable {
        Object output = joinPoint.proceed();
        try {
            List<MobileUserDto> userList = (List<MobileUserDto>) output;
            String appId = (String) joinPoint.getArgs()[0];
            String openId = (String) joinPoint.getArgs()[1];

            saveUserWxRelations(userList, appId, openId);
        } catch (Exception e) {
            log.error("保存微信小程序用户信息异常", e);
        }
        return output;
    }

    private void saveUserWxRelations(List<MobileUserDto> userList, String appId, String openId) {
        List<UserWxRelation> userWxRelationList = new ArrayList<>();
        userList.forEach(user -> {
            if (user.getTenantId() == null) {
                return;
            }
            String openIdSource = userWxRelationService.getOpenId(user.getId(), appId);
            if (openId.equalsIgnoreCase(openIdSource)) {
                return;
            }
            UserWxRelation userWxRelation = new UserWxRelation();
            userWxRelation.setAppId(appId);
            userWxRelation.setOpenId(openId);
            userWxRelation.setUserId(user.getId());
            userWxRelationList.add(userWxRelation);
        });
        if (userWxRelationList.size() > 0) {
            userWxRelationService.saveRelationList(userWxRelationList);
        }
    }

    @Around("execution(* com.dusk.module.auth.service.IWxMaService.getSession(..))")
    public Object aroundGetSession(ProceedingJoinPoint joinPoint) throws Throwable {
        Object output = joinPoint.proceed();
        try {
            WxMaSessionResult wxMaSessionResult = (WxMaSessionResult) output;
            if (wxMaSessionResult == null || wxMaSessionResult.getOpenid() == null
                    || wxMaSessionResult.getLoginData() == null || wxMaSessionResult.getLoginData().isEmpty()) {
                return output;
            }
            String appId = (String) joinPoint.getArgs()[0];
            String openId = wxMaSessionResult.getOpenid();

            saveUserWxRelations(wxMaSessionResult.getLoginData(), appId, openId);
        } catch (Exception e) {
            log.error("保存微信小程序用户信息异常", e);
        }
        return output;
    }
}
