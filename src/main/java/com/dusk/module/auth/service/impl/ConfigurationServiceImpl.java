package com.dusk.module.auth.service.impl;

import com.dusk.module.auth.dto.configuration.*;
import com.dusk.module.auth.service.*;
import com.github.dozermapper.core.Mapper;
import lombok.extern.slf4j.Slf4j;
import com.dusk.common.framework.dto.EntityDto;
import com.dusk.common.framework.model.UserContext;
import com.dusk.common.framework.service.CruxBaseServiceImpl;
import com.dusk.common.framework.tenant.TenantContextHolder;
import com.dusk.module.auth.common.config.AppAuthConfig;
import com.dusk.module.auth.common.manage.TokenAuthManager;
import com.dusk.module.auth.common.permission.IAuthPermissionManager;
import com.dusk.module.auth.dto.configuration.*;
import com.dusk.module.auth.dto.feature.FeatureConfigDto;
import com.dusk.module.auth.dto.user.GetUserForEditOutput;
import com.dusk.module.auth.dto.user.UserRoleDto;
import com.dusk.module.auth.entity.GrantPermission;
import com.dusk.module.auth.entity.Tenant;
import com.dusk.module.auth.entity.User;
import com.dusk.module.auth.repository.IGrantPermissionRepository;
import com.dusk.module.auth.repository.ITenantRepository;
import com.dusk.module.auth.repository.IUserRepository;
import com.dusk.module.auth.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author kefuming
 * @date 2020-05-07 15:29
 */
@Service
@Slf4j
public class ConfigurationServiceImpl extends CruxBaseServiceImpl implements IConfigurationService {
    @Autowired
    IUserRepository userRepository;
    @Autowired
    IAuthPermissionManager permissionManager;
    @Autowired
    IFeatureService featureService;
    @Autowired
    ITenantRepository tenantRepository;
    @Autowired
    IGrantPermissionRepository grantPermissionRepository;
    @Autowired
    private Mapper dozerMapper;
    @Autowired
    private ITenantPermissionService tenantPermissionService;
    @Autowired
    private ICaptchaService captchaService;
    @Autowired
    AppAuthConfig appAuthConfig;
    @Autowired
    private IUserService userService;
    @Autowired
    private TokenAuthManager tokenAuthManager;
    @Autowired
    IDynamicMenuService dynamicMenuService;

    /**
     * 注意这个接口允许匿名访问 所以存在没有登陆人信息的情况
     *
     * @return
     */
    @Override
    public ConfigurationDto getAll(HttpServletRequest request) {

        UserContext userContext = null;
        try {
            userContext = tokenAuthManager.checkTokenValid(request);
        } catch (Exception ex) {
            log.info(ex.getMessage());
        }
        //获取用户所有得权限
        ConfigurationDto configurationDto = new ConfigurationDto();
        configurationDto.setAuth(getAuthConfig(userContext));
        configurationDto.setCurrentUser(userContext);

        if (userContext != null) {
            configurationDto.setUserInfo(userService.getUserForEdit(new EntityDto(userContext.getId())));
        }
        //获取租户特性
        configurationDto.setFeatureConfig(getFeatureConfig());
        configurationDto.setTenantConfig(getTenantConfig());
        configurationDto.setLoginInfo(getLoginInfo(request));
        configurationDto.setDynamicMenus(getDynamicMenus(configurationDto.getUserInfo()));
        return configurationDto;
    }

    //获取动态菜单
    private List<DynamicMenuDto> getDynamicMenus(GetUserForEditOutput userInfo) {
        if (userInfo != null) {
            List<Long> roleIds = userInfo.getRoles().stream().filter(UserRoleDto::isAssigned).map(UserRoleDto::getRoleId).collect(Collectors.toList());
            if (!roleIds.isEmpty()) {
                return dynamicMenuService.getDynamicMenus(roleIds);
            }
        }
        return null;
    }

    private LoginInfoDto getLoginInfo(HttpServletRequest request) {
        LoginInfoDto dto = new LoginInfoDto();
        dto.setNeedCaptcha(captchaService.checkNeedCaptcha(request));
        return dto;
    }

    private List<AuthConfigDto> getAuthConfig(UserContext userContext) {
        List<String> grantPermissions = new ArrayList<>();
        User loginUser = null;
        if (userContext != null) {
            Optional<User> userOptional = userRepository.findById(userContext.getId());
            if (userOptional.isPresent()) {
                loginUser = userOptional.get();
                List<Long> roleIds = loginUser.getUserRoles().stream().map(p -> p.getId()).collect(Collectors.toList());
                if (roleIds.size() > 0) {
                    List<GrantPermission> grantPermissionList = grantPermissionRepository.findDistinctByRoleIdIn(roleIds.toArray(new Long[0]));
                    grantPermissionList.stream().map(p -> p.getName()).collect(Collectors.toList()).forEach(p -> {
                        if (!grantPermissions.contains(p)) {
                            grantPermissions.add(p);
                        }
                    });
                }
            } else {
                userContext = null;
            }
        }

        List<String> permissions;
        //当配置不过滤租户权限
        if (appAuthConfig.isDisableTenantAuthFilter()) {
            permissions = permissionManager.getDefinitionPermission(TenantContextHolder.getTenantId() != null);
        } else {
            //当宿主进入访问获取所有的权限清单
            if (TenantContextHolder.getTenantId() == null) {
                permissions = permissionManager.getDefinitionPermission(false);
            } else {
                //租户进来获取有权限的清单
                permissions = tenantPermissionService.getGrantedPermissionByTenantId(TenantContextHolder.getTenantId());
            }
        }

        List<AuthConfigDto> authConfigDtos = new ArrayList<>();
        for (String permissionCode : permissions) {
            AuthConfigDto dto = new AuthConfigDto();
            dto.setPermissionCode(permissionCode);
            if (loginUser != null && loginUser.isAdmin()) {
                dto.setGranted(true);
            } else {
                if (grantPermissions.contains(permissionCode)) {
                    dto.setGranted(true);
                }
            }
            //仅仅返回 有权限的清单
            if (dto.isGranted()) {
                authConfigDtos.add(dto);
            }
        }
        return authConfigDtos;
    }

    private FeatureConfigDto getFeatureConfig() {
        FeatureConfigDto featureConfig = new FeatureConfigDto();
//        featureConfig.setFeatureDto(featureService.getTenantFeatures());
        featureConfig.setAllFeatures(featureService.getTenantFeatures());
        return featureConfig;
    }

    private TenantConfigDto getTenantConfig() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            Optional<Tenant> tenantOptional = tenantRepository.findById(tenantId);
            if (tenantOptional.isPresent()) {
                return dozerMapper.map(tenantOptional.get(), TenantConfigDto.class);
            }
        }
        return null;
    }
}
