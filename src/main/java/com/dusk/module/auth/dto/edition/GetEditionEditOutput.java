package com.dusk.module.auth.dto.edition;

import com.dusk.module.auth.dto.TenantFeature;
import lombok.Getter;
import lombok.Setter;
import com.dusk.module.auth.dto.feature.FeatureValueInput;

import java.util.List;

/**
 * @author kefuming
 * @date 2020-05-08 13:51
 */
@Getter
@Setter
public class GetEditionEditOutput {
    private EditionEditDto edition;

    private List<FeatureValueInput> featureValues;

    private List<TenantFeature> features;
}
