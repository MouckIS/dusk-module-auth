package com.dusk.module.auth.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @author kefuming
 * @date 2020-12-25 9:32
 */
@Configuration
@Getter
@Setter
public class AppAuthConfig {
    /**
     * 是否禁止过滤租户权限
     */
    @Value("${app.disable-tenant-auth-filter:false}")
    boolean disableTenantAuthFilter;

    @Value("${app.login.encrypt-key}")
    String loginEncryptKey;
}
