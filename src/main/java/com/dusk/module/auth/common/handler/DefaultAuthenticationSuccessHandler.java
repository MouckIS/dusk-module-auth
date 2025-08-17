package com.dusk.module.auth.common.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.dusk.common.framework.model.UserContext;
import com.dusk.common.framework.response.BaseApiResult;
import com.dusk.module.auth.common.manage.TokenAuthManager;
import com.dusk.module.auth.service.ICaptchaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author kefuming
 * @date 2020-04-23 8:37
 */
@Component
public class DefaultAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private TokenAuthManager tokenAuthManager;
    @Autowired
    private ICaptchaService captchaService;


    @Override
    public void onAuthenticationSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) throws IOException, ServletException {
        UserContext userContext = (UserContext) authentication.getPrincipal();
        String accessToken = tokenAuthManager.generateToken(userContext);
        httpServletResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
        httpServletResponse.setCharacterEncoding(StandardCharsets.UTF_8.toString());
        mapper.writeValue(httpServletResponse.getWriter(), BaseApiResult.success(accessToken, "登录成功"));
        clearAuthenticationAttributes(httpServletRequest);

        //清除错误缓存
        captchaService.clearBuffer(httpServletRequest);
    }

    private void clearAuthenticationAttributes(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }
        session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
    }
}
