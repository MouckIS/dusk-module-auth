package com.dusk.module.auth.dto.feature;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * TODO:实现特性
 * @author kefuming
 * @date 2020-05-07 15:26
 */
@Data
public class FeatureConfigDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -5127662938662005864L;
    private Map<String,Map<String,String>> allFeatures;
}
