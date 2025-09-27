package com.dusk.module.ddm.module.auth.dto.setting;

import lombok.Getter;
import lombok.Setter;
import com.dusk.common.core.dto.NameValueDto;

import java.util.ArrayList;
import java.util.List;

/**
 * @author kefuming
 * @date 2020/11/30 8:03
 */
@Getter
@Setter
public class UpdateSettingInput {
    List<NameValueDto<String>> nameValues = new ArrayList<>();
}
