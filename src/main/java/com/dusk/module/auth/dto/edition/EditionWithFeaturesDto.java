package com.dusk.module.auth.dto.edition;

import com.dusk.common.core.dto.NameValueDto;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author kefuming
 * @date 2020-05-08 10:33
 */
@Getter
@Setter
public class EditionWithFeaturesDto {

    public EditionSelectDto edition;

    public List<NameValueDto<String>> featureValues = new ArrayList<>();
}
