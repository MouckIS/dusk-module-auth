package com.dusk.module.auth.dto.edition;

import lombok.Data;
import com.dusk.common.framework.feature.ui.TenantFeature;
import com.dusk.module.auth.dto.feature.FeatureValueInput;

import java.util.List;

/**
 * @author kefuming
 * @date 2020-05-08 13:51
 */
@Data
public class GetEditionEditOutput {
    private EditionEditDto edition;

    private List<FeatureValueInput> featureValues;

    private List<TenantFeature> features;
}
