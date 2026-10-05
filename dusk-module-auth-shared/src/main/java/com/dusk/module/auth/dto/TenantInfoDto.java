package com.dusk.module.auth.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author 王吉
 * @date 2020-07-24 16:31
 */
@Getter
@Setter
public class TenantInfoDto implements Serializable {
    /**
     * 租户是否可用
     */
    private boolean enabled;
    /**
     * 租户代码
     */
    private String tenantName;

    /**
     * 租户名
     */
    private String name;

    /**
     * id
     */
    private Long id;

    /**
     * 描述
     */
    private String description;

    /**
     * app下载地址
     */
    private String appDownloadUrl;

    /**
     * app版本号，1.2.3.xxxx, 版本号的比较只关注前三级
     */
    private String appVersion;
}
