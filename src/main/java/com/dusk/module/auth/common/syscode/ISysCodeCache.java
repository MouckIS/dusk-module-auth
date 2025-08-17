package com.dusk.module.auth.common.syscode;

import com.dusk.common.framework.syscode.SysCodeDefinitionDto;
import com.dusk.common.module.auth.dto.syscode.SysCodeTypeDto;

import java.util.List;
import java.util.Map;

/**
 * @author kefuming
 * @date 2021-08-16 11:04
 */
public interface ISysCodeCache {
    void pushSysCode(String applicationName, Map<String, SysCodeDefinitionDto> definition);

    List<SysCodeTypeDto> getSysCodeType();
}
