package com.dusk.module.auth.common.config;

import com.dusk.module.auth.common.filter.JWTAuthenticationFilter;
import com.dusk.module.auth.common.handler.DefaultAuthenticationFailureHandler;
import com.dusk.module.auth.common.handler.DefaultAuthenticationSuccessHandler;
import com.dusk.module.auth.common.provider.DefaultAuthenticationProvider;
import com.dusk.module.auth.service.ICaptchaService;
import tools.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * @author kefuming
 * @date 2020-05-20 15:41
 */
@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    private static final String AUTHENTICATION_URL = "/login";
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private ICaptchaService captchaService;
    @Resource
    private ApplicationEventPublisher publisher;
    @Resource
    private DefaultAuthenticationSuccessHandler authenticationSuccessHandler;
    @Resource
    private DefaultAuthenticationProvider authenticationProvider;
    @Resource
    private DefaultAuthenticationFailureHandler authenticationFailureHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        //拦截请求路由
                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .sessionManagement(sm -> sm
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }

    /* ================ 2. WebSecurity 忽略静态路由 ================ */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring()
                .requestMatchers("/actuator/**", "/error/**");
    }

    /* ================ 3. 自定义 JWT 过滤器 ================ */
    private JWTAuthenticationFilter jwtAuthenticationFilter() {
        JWTAuthenticationFilter filter = new JWTAuthenticationFilter(
                AUTHENTICATION_URL,
                authenticationSuccessHandler,
                authenticationFailureHandler,
                objectMapper,
                captchaService,
                publisher);
        filter.setAuthenticationManager(authenticationManager());
        return filter;
    }

    /* ================ 4. AuthenticationManager ================ */
    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(authenticationProvider);
    }

    /* ================ 5. 密码编码器 ================ */

    /**
     * 密码编码器：<b>必须</b>是裸 BCrypt，不能换成 {@code PasswordEncoderFactories} 的 Delegating 版本。
     *
     * <p>库里的 {@code sys_user.password} 是历史写入的裸 bcrypt 串（例如
     * {@code $2a$10$6wES...}，见 {@code V1__init_20200630.sql}），没有 {@code {bcrypt}} 前缀；
     * Delegating 版本对无前缀串会走 {@code UnmappedIdPasswordEncoder} 并抛
     * {@code IllegalArgumentException: There is no PasswordEncoder mapped for the id "null"}，
     * 导致所有历史账号无法登录。单体化前该 bean 实际由已删除的 {@code DuskAuthApplication}
     * 提供（同名 bean 覆盖了这里的 Delegating 版本），合并后必须在此显式保留 BCrypt。</p>
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
