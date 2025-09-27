package com.dusk.module.ddm.module.auth.dto.setting.tenants;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author kefuming
 * @date 2020/11/17 11:33
 */
@Getter
@Setter
@NoArgsConstructor
public class LdapSettingsDto extends LdapSettingsEditDto{
    public LdapSettingsDto(boolean moduleEnabled){
        super(moduleEnabled);
    }
}
