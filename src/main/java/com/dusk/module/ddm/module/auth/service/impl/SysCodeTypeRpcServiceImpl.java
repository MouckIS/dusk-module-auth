package com.dusk.module.ddm.module.auth.service.impl;

import org.apache.dubbo.config.annotation.Service;
import com.dusk.common.module.auth.dto.syscode.SysCodeTypeDto;
import com.dusk.common.module.auth.service.ISysCodeTypeRpcService;
import com.dusk.module.auth.service.ISysCodeTypeService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * @author kefuming
 * @date 2021-01-28 13:35
 */
@Service
public class SysCodeTypeRpcServiceImpl implements ISysCodeTypeRpcService {
    @Autowired
    ISysCodeTypeService sysCodeTypeService;

    @Override
    public List<SysCodeTypeDto> list() {
        return sysCodeTypeService.list();
    }

    @Override
    public Long totalCount() {
        return (long) list().size();
    }
}
