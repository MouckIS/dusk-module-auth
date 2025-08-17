package com.dusk.module.auth.service;

import com.dusk.common.module.auth.dto.syscode.SysCodeTypeDto;

import java.util.List;

/**
 * @author kefuming
 * @date 2021-01-28 13:36
 */
public interface ISysCodeTypeService {
    /**
     * 查询配置类型定义
     */
    List<SysCodeTypeDto> list();
}
