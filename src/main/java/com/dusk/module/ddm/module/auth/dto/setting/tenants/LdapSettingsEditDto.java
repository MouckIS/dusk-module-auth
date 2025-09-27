package com.dusk.module.ddm.module.auth.dto.setting.tenants;

import lombok.Data;

/**
 * @author kefuming
 * @date 2020-06-16 8:02
 */
@Data
public class LdapSettingsEditDto {
    private boolean moduleEnabled ;

    private boolean enabled ;

    private String domain ;

    private String userName ;

    private String password ;

    public LdapSettingsEditDto(){

    }

    public LdapSettingsEditDto(boolean moduleEnabled){
        this.moduleEnabled = moduleEnabled;
    }
}
