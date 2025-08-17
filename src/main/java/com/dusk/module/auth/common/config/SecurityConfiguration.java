package com.dusk.module.auth.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import com.dusk.module.auth.common.filter.JWTAuthenticationFilter;
import com.dusk.module.auth.common.handler.DefaultAuthenticationFailureHandler;
import com.dusk.module.auth.common.handler.DefaultAuthenticationSuccessHandler;
import com.dusk.module.auth.common.provider.DefaultAuthenticationProvider;
import com.dusk.module.auth.service.ICaptchaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * @author kefuming
 * @date 2020-05-20 15:41
 */
@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    private static final String AUTHENTICATION_URL = "/login";

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ICaptchaService captchaService;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private DefaultAuthenticationSuccessHandler authenticationSuccessHandler;

    @Autowired
    private DefaultAuthenticationProvider authenticationProvider;

    @Autowired
    private DefaultAuthenticationFailureHandler authenticationFailureHandler;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .cors().and().csrf().disable()
                .requestMatchers().anyRequest()
                .and()
                .authorizeRequests()
                //拦截请求路由
                .anyRequest()
                .permitAll()
                .and()
                .addFilterBefore(buildJWTAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    }

    @Override
    public void configure(WebSecurity web) {
        //忽略路由，不进入spring security 拦截链
        //TODO:做成配置
        web.ignoring().antMatchers("/actuator/**", "/error/**");
    }

    @SneakyThrows
    private JWTAuthenticationFilter buildJWTAuthenticationFilter() {
        JWTAuthenticationFilter filter = new JWTAuthenticationFilter(AUTHENTICATION_URL, authenticationSuccessHandler, authenticationFailureHandler, objectMapper, captchaService, publisher);
        filter.setAuthenticationManager(authenticationManagerBean());
        return filter;
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(authenticationProvider);
    }

    /**
     * 将 AuthenticationManager 注册为 bean , 方便配置 oauth server 的时候使用
     */
    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }
}
