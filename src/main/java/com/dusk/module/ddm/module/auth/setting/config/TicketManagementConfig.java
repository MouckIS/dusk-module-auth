package com.dusk.module.ddm.module.auth.setting.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author kefuming
 * @date 2020-06-16 12:03
 */
@Configuration
@ConfigurationProperties(prefix = "app.setting.ticket")
@Data
public class TicketManagementConfig {
    private String unAnalyzeDeviceBoardTerm = "false";
    private String filterSpareParts = "true";
    private String historyMarkDisorder = "false";
    private String historyMarkMustAll = "true";
}
