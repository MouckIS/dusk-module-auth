package com.dusk.module.auth.isc;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * @author kefuming
 * @date 2021-11-19 9:22
 */
@Configuration
@ConfigurationProperties("isc.sync")
@Getter
@Setter
public class IscSyncConfig {
    private boolean enable = false;
    private String endpoint;
    /**
     * 用户的默认密码
     */
    private String defaultPassword = "123456";

    private List<Application> applications = new ArrayList<>();

    /**
     * 微信服务的配置项
     */
    @Getter
    @Setter
    public static class Application
    {
        /**
         * 配置项代码 (ProductCode)
         */
        private String appid;

        /**
         * 租户名
         */
        private String tenantName;

        /**
         * 用户的默认密码
         */
        private String defaultPassword;
    }
}
