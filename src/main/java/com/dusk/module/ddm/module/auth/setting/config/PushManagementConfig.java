package com.dusk.module.ddm.module.auth.setting.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author kefuming
 * @date 2020-05-22 13:45
 */
@Configuration
@ConfigurationProperties(prefix = "app.setting.push")
@Data
public class PushManagementConfig {
    private Mobile mobile = new Mobile();
    @Data
    public static class Mobile{
        private String iosAppKey;
        private String androidAppKey;
        private String secret;
        private Pages pages = new Pages();

        @Data
        public static class Pages{
            private String keyCabinetDetail;
            private String wireCabinetDetail;
            private String safetyWearDetail;
        }
    }
}
