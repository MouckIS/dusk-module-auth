package com.dusk.module.auth.impl;

import com.dusk.common.core.auth.IAuthProvider;
import com.dusk.common.core.auth.permission.Permission;
import com.dusk.common.core.auth.permission.UrlPermission;
import com.dusk.module.auth.service.IAuthRpcService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @author kefuming
 * @date 2020-05-25 15:44
 */
@Component
@Slf4j
public class DefaultAuthProvider implements IAuthProvider {
    @DubboReference(timeout = 5000, retries = 0)
    public IAuthRpcService authService;

    /**
     * 简单支持异步重试，如要优雅可以引入guava里的retry包
     *
     * @param applicationName
     * @param allowAnonymousPath
     * @param definitionPermissions
     * @param urlPermissions
     */
    @Async
    @Override
    public void provideAuthInfo(String applicationName, List<String> allowAnonymousPath, Map<String, Permission> definitionPermissions, Map<String, List<UrlPermission>> urlPermissions) {
        while (true) {
            try {
                authService.provideAuthInfo(applicationName, allowAnonymousPath, definitionPermissions, urlPermissions);
                return;
            } catch (Exception ex) {
                log.error("推送权限清单异常：{}", ex.getMessage());
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException iex) {
                    log.warn("终止推送权限清单！");
                }
            }
        }
    }
}
