package com.dusk.module.auth.common.manage;

import com.dusk.common.core.model.UserContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author kefuming
 * @date 2020-05-25 15:09
 */
@Component
public class DefaultAccessDecisionManager {

    public boolean decide(UserContext authentication, Collection<ConfigAttribute> configAttributes) throws AccessDeniedException, InsufficientAuthenticationException {
        if (configAttributes == null || configAttributes.size() == 0) {
            return true;
        }
        Iterator<ConfigAttribute> ite = configAttributes.iterator();
        while (ite.hasNext()) {
            ConfigAttribute ca = ite.next();
            String needRole = ca.getAttribute();
            for (GrantedAuthority ga : authentication.getAuthorities()) {
                if (needRole.equals(ga.getAuthority()))
                    return true;
            }
        }
       return  false;
    }
}
