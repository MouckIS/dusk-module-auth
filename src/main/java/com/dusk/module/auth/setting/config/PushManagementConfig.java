package com.dusk.module.auth.setting.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author kefuming
 * @date 2020-05-22 13:45
 */
@Configuration
@ConfigurationProperties(prefix = "app.setting.push")
@Getter
@Setter
public class PushManagementConfig {
    private Mobile mobile = new Mobile();

    @Getter
    @Setter
    public static class Mobile {
        private String iosAppKey;
        private String androidAppKey;
        private String secret;
        private Pages pages = new Pages();

        @Getter
        @Setter
        public static class Pages {
            private String keyCabinetDetail;
            private String wireCabinetDetail;
            private String safetyWearDetail;
        }
    }
}
