package com.dusk.module.auth.dto.setting.tenants;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


/**
 * @author kefuming
 * @date 2020-06-16 8:15
 */
@Data
public class TenantSettingsDto {
    private GeneralSettingsDto general;

    private TenantUserManagementSettingsDto userManagement;

    private EmailSettingsDto email;

    private LdapSettingsDto ldap;

    private SecuritySettingsDto security;

    private TenantBillingSettingsDto billing;

    private TenantTicketSettingsDto ticket;

    @Schema(description = "外观配置")
    private FacadeSettingDto facadeSetting;
}
