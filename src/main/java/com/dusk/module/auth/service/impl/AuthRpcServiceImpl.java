package com.dusk.module.auth.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.Service;
import org.apache.dubbo.rpc.RpcContext;
import com.dusk.common.framework.auth.permission.Permission;
import com.dusk.common.framework.auth.permission.UrlPermission;
import com.dusk.common.framework.dto.NameValueDefaultByDto;
import com.dusk.common.framework.dto.NameValueDto;
import com.dusk.common.framework.exception.UserContextException;
import com.dusk.common.framework.feature.IFeatureChecker;
import com.dusk.common.framework.jwt.JwtTokenFactory;
import com.dusk.common.framework.jwt.extractor.JwtHeaderTokenExtractor;
import com.dusk.common.framework.model.UserContext;
import com.dusk.common.framework.tenant.TenantContextHolder;
import com.dusk.common.framework.utils.SecurityUtils;
import com.dusk.common.framework.utils.UserContextUtils;
import com.dusk.common.module.auth.service.IAuthRpcService;
import com.dusk.module.auth.common.datafilter.IDataFilterDefinitionContext;
import com.dusk.module.auth.common.manage.DefaultAccessDecisionManager;
import com.dusk.module.auth.common.manage.TokenAuthManager;
import com.dusk.module.auth.common.metadata.DefaultInvocationSecurityMetadataSource;
import com.dusk.module.auth.common.permission.IAuthPermissionManager;
import com.dusk.module.auth.common.provider.CustomAuthProvider;
import com.dusk.module.auth.common.skiprequest.SkipPathRequestMatcher;
import com.dusk.module.auth.dto.station.StationsOfLoginUserDto;
import com.dusk.module.auth.feature.CenterControlFeatureProvider;
import com.dusk.module.auth.service.IStationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author kefuming
 * @date 2020-05-22 14:01
 */
@Service
@Slf4j
public class AuthRpcServiceImpl implements IAuthRpcService {
    @Autowired
    JwtTokenFactory jwtTokenFactory;
    @Autowired
    JwtHeaderTokenExtractor jwtHeaderTokenExtractor;
    @Autowired
    IAuthPermissionManager permissionManager;
    @Autowired
    UserContextUtils userContextUtils;
    @Autowired
    DefaultAccessDecisionManager accessDecisionManager;
    @Autowired
    DefaultInvocationSecurityMetadataSource metadataSource;
    @Autowired
    CustomAuthProvider customAuthProvider;
    @Autowired
    SkipPathRequestMatcher skipPathRequestMatcher;
    @Autowired
    IDataFilterDefinitionContext dataFilterDefinitionContext;
    @Autowired
    TokenAuthManager tokenAuthManager;
    @Autowired
    SecurityUtils securityUtils;
    @Autowired
    IStationService stationService;
    @Autowired
    IFeatureChecker featureChecker;

    @Override
    public boolean auth(String authorization, String applicationName, String url) {
        if (ignoreAuthentication(applicationName, url)) {
            return true;
        } else {
            return hasPermission(authorization, applicationName, url);
        }

    }

    @Override
    public void provideAuthInfo(String applicationName, List<String> allowAnonymousPath, Map<String, Permission> definitionPermissions, Map<String, List<UrlPermission>> urlPermissions) {
        customAuthProvider.provideAuthInfo(applicationName, allowAnonymousPath, definitionPermissions, urlPermissions);
    }

    @Override
    public UserContext getUserContext() {
        String authorization = RpcContext.getContext().getAttachment("authorization");
        UserContext userContext = null;
        if (!StringUtils.isEmpty(authorization)) {
            try {
                userContext = jwtTokenFactory.parseJwtToken(jwtHeaderTokenExtractor.extract(authorization));
            } catch (Exception ex) {
                log.info(ex.getMessage());
            }
        }
        return userContext;
    }

    @Override
    public String getLinkedOrgIds(String orgId, String authentication) {
        if (StrUtil.isNotEmpty(authentication)) {
            UserContext userContext = userContextUtils.getUserContext(authentication);
            if (userContext != null) {
                try {
                    TenantContextHolder.setTenantId(userContext.getTenantId());
                    if (featureChecker.isEnabled(CenterControlFeatureProvider.STATION_DOWNWARD)) {
                        return orgId;
                    }
                    //如果当前用户登陆了，则获取关联的默认厂站
                    if (StrUtil.isEmpty(orgId)) {
                        //这里走分支，如果由特性 则获取所有厂站，没有则获取默认第一个厂站
                        List<StationsOfLoginUserDto> stations = stationService.getStationsForFrontByUserId(userContext.getId());
                        if (featureChecker.isEnabled(CenterControlFeatureProvider.STATION_CENTER_CONTROL)) {
                            List<Long> allOrg = new ArrayList<>();
                            List<Long> collect = stations.stream().map(NameValueDto::getValue).collect(Collectors.toList());
                            collect.forEach(p -> {
                                List<Long> linkOrgs = dataFilterDefinitionContext.getDataFilterDefinition().get(p.toString());
                                linkOrgs.forEach(q -> {
                                    if (!allOrg.contains(q)) {
                                        allOrg.add(q);
                                    }
                                });
                            });
                            if (allOrg.size() > 0) {
                                return CollectionUtil.join(allOrg, ",");
                            }
                            return null;
                        } else {
                            //非集控模式走默认代码
                            Optional<StationsOfLoginUserDto> defaultStation = stations.stream().filter(NameValueDefaultByDto::isDefaultBy).findFirst();
                            if (defaultStation.isPresent()) {
                                orgId = defaultStation.get().getValue().toString();
                            } else {
                                if (stations.size() > 0) {
                                    orgId = stations.get(0).getValue().toString();
                                }
                            }
                        }
                    }

                } finally {
                    TenantContextHolder.clear();
                }
            }
        }
        if (!StringUtils.isEmpty(orgId)) {
            List<Long> ids = dataFilterDefinitionContext.getDataFilterDefinition().get(orgId);
            if (ids != null && ids.size() > 0) {
                Long[] orgIdArr = new Long[ids.size()];
                ids.toArray(orgIdArr);
                return ArrayUtil.join(orgIdArr, ",");
            }
        }

        return null;
    }

    @Override
    public String changeRealToken(String tokenId) {
        return tokenAuthManager.getToken(tokenId);
    }


    //region private method

    /**
     * 是否跳过权限验证
     *
     * @param url
     * @return
     */
    private boolean ignoreAuthentication(String applicationName, String url) {
        return skipPathRequestMatcher.matches(applicationName, url);
    }

    private boolean hasPermission(String authorization, String applicationName, String url) {
        UserContext userContext = tokenAuthManager.checkTokenValid(authorization);
        if (userContext == null) {
            throw new UserContextException("尚未登陆");
        }
        Collection<ConfigAttribute> attributes = metadataSource.getAttributes(applicationName, url);
        return accessDecisionManager.decide(userContext, attributes);
    }

    //endregion
}
