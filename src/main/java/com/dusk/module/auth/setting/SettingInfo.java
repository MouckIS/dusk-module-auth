package com.dusk.module.auth.setting;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author kefuming
 * @date 2020-05-21 8:20
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettingInfo implements Serializable {
    private Long tenantId;
    private Long stationId;
    private Long userId;
    private String name;
    private String value;
}
