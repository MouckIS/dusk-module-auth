package com.dusk.module.ddm.module.auth.dto.setting.tenants;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.timing.Clock;
import com.dusk.module.auth.dto.setting.host.EmailSettingsEditDto;
import com.dusk.module.auth.dto.setting.host.GeneralSettingsEditDto;
import com.dusk.module.auth.dto.setting.host.SecuritySettingsEditDto;

import java.util.ArrayList;
import java.util.List;


/**
 * @author kefuming
 * @date 2020-06-16 8:15
 */
@Data
public class TenantSettingsEditDto {
    private GeneralSettingsEditDto general;

    private TenantUserManagementSettingsEditDto userManagement;

    private EmailSettingsEditDto email;

    private LdapSettingsEditDto ldap;

    private SecuritySettingsEditDto security;

    private TenantBillingSettingsEditDto billing;

    private TenantTicketSettingsEditDto ticket;

    @ApiModelProperty("外观配置")
    private FacadeSettingEditDto facadeSetting;

    /**
     * This validation is done for single-tenant applications.
     * Because, these settings can only be set by tenant in a single-tenant application.
     */
    public void validateHostSettings()
    {
        List<String> validationErrors = new ArrayList<String>();
        if (Clock.isSupportsMultipleTimezone() && general == null)
        {
            validationErrors.add("General settings can not be null.");
        }

        if (email == null)
        {
            validationErrors.add("Email settings can not be null.");
        }

        if (validationErrors.size() > 0)
        {
            throw new BusinessException("Method arguments are not valid! See ValidationErrors for details:" + validationErrors);
        }
    }
}
