package com.dusk.module.auth.dto;

import com.dusk.module.ddm.dto.ui.InputType;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author duanxiaokang
 * @version 0.0.1
 * @date 2020/4/30 9:02
 */
@Data
public class TenantFeature implements Serializable {
    @Serial
    private static final long serialVersionUID = -7653253401033370895L;
    String parentName;
    String name;
    String featureValue;
    String displayName;
    String description;
    String defaultValue;
    InputType inputType;
}
