package com.dusk.module.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication(scanBasePackages = {
        "com.dusk.module.auth",
        "com.dusk.common.core",
        "com.dusk.common.rpc"
})
@EnableAsync
public class DuskAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(DuskAuthApplication.class, args);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
