package com.dusk.module.auth.adapter;

import com.dusk.common.core.auth.IAuthProvider;
import com.dusk.common.core.auth.permission.Permission;
import com.dusk.common.core.auth.permission.UrlPermission;
import com.dusk.module.auth.service.IAuthRpcService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired
    public IAuthRpcService authService;

    /**
     * 推送权限清单。
     *
     * <p>单体化前这是一次跨进程 Dubbo 调用，失败很常见，所以原实现用
     * {@code while (true) + Thread.sleep(10000)} 无限重试；现在 {@code authService} 是同进程内的
     * 本地 bean（{@code AuthRpcServiceImpl}），调用不再有网络失败的可能，永久重试只会在
     * 真正有 bug 时无声地占满 {@code @Async} 线程池。因此改为单次调用，异常只记录日志。</p>
     */
    @Async
    @Override
    public void provideAuthInfo(String applicationName, List<String> allowAnonymousPath, Map<String, Permission> definitionPermissions, Map<String, List<UrlPermission>> urlPermissions) {
        try {
            authService.provideAuthInfo(applicationName, allowAnonymousPath, definitionPermissions, urlPermissions);
        } catch (Exception ex) {
            log.error("推送权限清单异常：{}", ex.getMessage(), ex);
        }
    }
}
