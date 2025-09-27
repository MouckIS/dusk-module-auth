package com.dusk.module.auth.common.util;

import lombok.experimental.UtilityClass;
import com.dusk.common.core.constant.AuthConstant;
import com.dusk.common.core.model.UserContext;
import com.dusk.module.auth.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author kefuming
 * @date 2020-12-21 8:10
 */
@UtilityClass
public class LoginUtils {

    /**
     * 把用户数据转换成用户上下文
     *
     * @param user
     * @return
     */
    public UserContext getUserContextByUser(User user) {
        UserContext context = new UserContext();
        //写入数据
        context.setId(user.getId());
        context.setTenantId(user.getTenantId());
        context.setName(user.getName());
        context.setIsAdmin(user.isAdmin());
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (var userRole : user.getUserRoles()) {
            authorities.add(new SimpleGrantedAuthority(AuthConstant.TYPE_ROLE + userRole.getId()));
        }
        if (user.isAdmin()) {
            if (!StringUtils.isEmpty(user.getTenantId())) {
                authorities.add(new SimpleGrantedAuthority(AuthConstant.ROLE_TENANT_ADMIN + user.getTenantId()));
            } else {
                authorities.add(new SimpleGrantedAuthority(AuthConstant.ROLE_HOST_ADMIN));
            }

        }
        context.setAuthorities(authorities);
        return context;
    }
}
