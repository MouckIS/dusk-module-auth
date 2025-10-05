package com.dusk.module.auth.dto.configuration;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author kefuming
 * @date 2020-05-18 15:26
 */
@Getter
@Setter
public class TenantConfigDto implements Serializable {
    private Long id;
    private String tenantName;
    private String name;
    private String description;
    private String appDownloadUrl;
}
