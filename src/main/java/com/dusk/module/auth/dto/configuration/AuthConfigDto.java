package com.dusk.module.auth.dto.configuration;

import lombok.Data;

import java.io.Serializable;

/**
 * @author kefuming
 * @date 2020-05-07 15:23
 */
@Data
public class AuthConfigDto implements Serializable {
    private static final long serialVersionUID = -185939673621967905L;

    /**
     * 权限code
     */
    private String permissionCode;
    /**
     * 是否授权
     */
    private boolean granted;
}
