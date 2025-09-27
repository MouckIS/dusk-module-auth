package com.dusk.module.ddm.module.auth.dto.edition;

import lombok.Data;
import com.dusk.module.auth.dto.feature.FeatureValueInput;

import java.util.ArrayList;
import java.util.List;

/**
 * @author kefuming
 * @date 2020-05-08 10:10
 */
@Data
public class CreateOrUpdateEditionDto {
    private EditionEditDto editionEditDto = new EditionEditDto();
    private List<FeatureValueInput> featureValues = new ArrayList<>();
}
