package com.dusk.module.auth.dto.setting.host;

import lombok.Data;

/**
 * @author kefuming
 * @date 2020-05-21 15:44
 */
@Data
public class HostSettingsEditDto {
    private GeneralSettingsEditDto general;

    private HostUserManagementSettingsEditDto userManagement;

    private EmailSettingsEditDto email;

    private TenantManagementSettingsEditDto tenantManagement;

    private SecuritySettingsEditDto security;

    private HostBillingSettingsEditDto billing;

    private PushSettingEditDto push;
}
