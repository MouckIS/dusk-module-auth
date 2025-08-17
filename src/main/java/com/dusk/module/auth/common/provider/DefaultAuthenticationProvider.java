package com.dusk.module.auth.common.provider;

import com.dusk.common.framework.model.UserContext;
import com.dusk.module.auth.common.util.LoginUtils;
import com.dusk.module.auth.entity.User;
import com.dusk.module.auth.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * @author kefuming
 * @date 2020-05-20 16:53
 */
@Component
public class DefaultAuthenticationProvider implements AuthenticationProvider {
    @Autowired
    IUserService userService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Assert.notNull(authentication, "No authentication data provided");
        //取出传递的 账户密码上下文
        String username = (String) authentication.getPrincipal();
        String password = (String) authentication.getCredentials();
        User user = userService.checkAndGetUser(username, password);
        UserContext context = LoginUtils.getUserContextByUser(user);
        return new UsernamePasswordAuthenticationToken(context, null, context.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return (UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication));
    }
}
