package com.dusk.module.auth.service;

import com.dusk.common.core.auth.permission.Permission;
import com.dusk.common.core.auth.permission.UrlPermission;
import com.dusk.common.core.model.UserContext;
import com.dusk.module.auth.dto.OpenAppAuthResult;

import java.util.List;
import java.util.Map;

/**
 * 权限鉴权服务
 *
 * @author kefuming
 * @date 2020-05-22 13:53
 */
public interface IAuthRpcService {
    /**
     * 校验接口是否有权限
     *
     * @param applicationName
     * @param url
     * @return
     */
    boolean auth(String authorization, String applicationName, String url);

    /**
     * 服务启动调用rpc接口 给auth服务提供权限清单
     *
     * @param applicationName
     * @param allowAnonymousPath
     * @param definitionPermissions
     */
    void provideAuthInfo(String applicationName, List<String> allowAnonymousPath, Map<String, Permission> definitionPermissions, Map<String, List<UrlPermission>> urlPermissions);

    /**
     * 获取用户上下文 未登录返回null
     *
     * @return
     */
    UserContext getUserContext();

    /**
     * 获取关联的组织信息
     *
     * @param orgId
     * @return
     */
    String getLinkedOrgIds(String orgId, String authentication);

    default OpenAppAuthResult authOpenApp(String application, String httpMethod, String url, String apKey, String appSecret, String appTimestamp) {
        throw new UnsupportedOperationException("该方法未实现");
    }

    default String changeRealToken(String tokenId) {
        throw new UnsupportedOperationException("该方法未实现");
    }
}
