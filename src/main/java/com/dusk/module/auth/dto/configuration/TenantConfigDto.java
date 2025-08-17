package com.dusk.module.auth.dto.configuration;

import lombok.Data;

import java.io.Serializable;

/**
 * @author kefuming
 * @date 2020-05-18 15:26
 */
@Data
public class TenantConfigDto implements Serializable {
    private Long id;
    private String tenantName;
    private String name;
    private String description;
    private String appDownloadUrl;
}
