package com.dusk.module.auth.dto.configuration;

import com.dusk.module.ddm.dto.DynamicMenuDto;
import lombok.Data;
import com.dusk.common.core.model.UserContext;
import com.dusk.module.auth.dto.feature.FeatureConfigDto;
import com.dusk.module.auth.dto.user.GetUserForEditOutput;

import java.io.Serializable;
import java.util.List;

/**
 * @author kefuming
 * @date 2020-05-07 15:22
 */
@Data
public class ConfigurationDto implements Serializable {
    private static final long serialVersionUID = -5769825586670502339L;

    private List<AuthConfigDto> auth;

    private UserContext currentUser;

    private FeatureConfigDto featureConfig;

    private TenantConfigDto tenantConfig;

    private LoginInfoDto loginInfo;

    private GetUserForEditOutput userInfo;

    private List<DynamicMenuDto> dynamicMenus;
}
