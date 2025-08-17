package com.dusk.module.auth.aspect;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import com.dusk.common.framework.model.UserContext;
import com.dusk.common.framework.utils.UserContextUtils;
import com.dusk.module.auth.enums.LoginLogType;
import com.dusk.module.auth.listener.LogInOutEvent;
import com.dusk.module.auth.repository.IUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

/**
 * @author kefuming
 * @date 2022/11/1
 */
@Slf4j
@Aspect
@Component
public class UserLogAspect {
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private IUserRepository userRepository;
    @Autowired
    private UserContextUtils userContextUtils;

    @Around("execution(* com.dusk.module.auth.common.manage.TokenAuthManager.removeToken(..))")
    public void logLogOut(ProceedingJoinPoint joinPoint) throws Throwable {
        LogInOutEvent logInOutEvent = new LogInOutEvent(LoginLogType.LOGIN_OUT, LocalDateTime.now(), true);
        try {
            fillLog(logInOutEvent, (String) joinPoint.getArgs()[0]);
        } catch (Exception e) {
            log.error("日志初始化参数异常：" + e.getMessage());
        }

        try {
            joinPoint.proceed();
        } catch (Exception e) {
            logInOutEvent.setSuccess(false);
            logInOutEvent.setMsg(StrUtil.subPre(e.getMessage(), 1000));
            throw e;
        } finally {
            eventPublisher.publishEvent(logInOutEvent);
        }
    }

    @Around("execution(* com.dusk.module.auth.controller.FaceController.authenticate(..))")
    public Object logFaceLogIn(ProceedingJoinPoint joinPoint) throws Throwable {
        LogInOutEvent logInOutEvent = new LogInOutEvent(LoginLogType.LOGIN_IN, LocalDateTime.now(), false);
        Object result;
        try {
            result = joinPoint.proceed();
            try {
                fillLog(logInOutEvent, (String) result);
            } catch (Exception e) {
                log.error("人脸登录日志初始化参数异常：" + e.getMessage());
            }
        } catch (Exception e) {
            logInOutEvent.setMsg(StrUtil.subPre(e.getMessage(), 1000));
            throw e;
        } finally {
            logInOutEvent.setSuccess(logInOutEvent.getUserName() != null);
            eventPublisher.publishEvent(logInOutEvent);
        }
        return result;
    }

    private void fillLog(LogInOutEvent logInOutEvent, String token) {
        if (StringUtils.isNoneBlank(token)) {
            UserContext userContext = userContextUtils.getUserContext(token);
            if (userContext != null) {
                userRepository.findById(userContext.getId()).ifPresent(e -> logInOutEvent.setUserName(e.getUserName()));
            }
        }
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = servletRequestAttributes.getRequest();
        logInOutEvent.setIp(ServletUtil.getClientIP(request));
        logInOutEvent.setBrowserInfo(request.getHeader("User-Agent"));
    }
}
