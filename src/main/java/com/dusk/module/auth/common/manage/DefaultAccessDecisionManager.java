package com.dusk.module.auth.common.manage;

import com.dusk.common.core.model.UserContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * @author kefuming
 * @date 2020-05-25 15:09
 */
@Component
public class DefaultAccessDecisionManager {

    // Spring Security 7 移除了 ConfigAttribute/SecurityConfig（access-decision 模型重构），
    // 本链路仅把角色标识作为 String 载体使用，直接以 Collection<String> 表达
    public boolean decide(UserContext authentication, Collection<String> configAttributes) throws AccessDeniedException, InsufficientAuthenticationException {
        if (configAttributes == null || configAttributes.isEmpty()) {
            return true;
        }
        for (String needRole : configAttributes) {
            for (GrantedAuthority ga : authentication.getAuthorities()) {
                if (needRole.equals(ga.getAuthority()))
                    return true;
            }
        }
        return false;
    }
}
