package com.dusk.module.auth.setting.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

/**
 * @author kefuming
 * @date 2020-06-17 8:30
 */
@Configuration
@ConfigurationProperties(prefix = "app.setting.multi-tenancy")
@Data
@RefreshScope
public class MultiTenancyConfig {
    @Value("${enable:true}")
    private boolean enable;
}
