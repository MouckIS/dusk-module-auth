package com.dusk.module.auth.dto.edition;

import lombok.Getter;
import lombok.Setter;
import com.dusk.module.auth.dto.feature.FeatureValueInput;

import java.util.ArrayList;
import java.util.List;

/**
 * @author kefuming
 * @date 2020-05-08 10:10
 */
@Getter
@Setter
public class CreateOrUpdateEditionDto {
    private EditionEditDto editionEditDto = new EditionEditDto();
    private List<FeatureValueInput> featureValues = new ArrayList<>();
}
