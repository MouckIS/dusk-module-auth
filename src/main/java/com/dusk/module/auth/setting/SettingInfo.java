package com.dusk.module.auth.setting;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author kefuming
 * @date 2020-05-21 8:20
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SettingInfo implements Serializable {
    private Long tenantId;
    private Long stationId;
    private Long userId;
    private String name;
    private String value;
}
