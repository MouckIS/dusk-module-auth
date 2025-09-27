package com.dusk.module.ddm.module.auth.service.impl;

import com.dusk.common.core.tenant.TenantContextHolder;
import com.dusk.common.module.auth.dto.syscode.SysCodeTypeDto;
import com.dusk.module.auth.common.syscode.ISysCodeCache;
import com.dusk.module.auth.service.ISysCodeTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SysCodeTypeService implements ISysCodeTypeService {

    @Autowired
    ISysCodeCache sysCodeCache;

    @Override
    public List<SysCodeTypeDto> list() {
        List<SysCodeTypeDto> collect = sysCodeCache.getSysCodeType().stream().sorted(Comparator.comparing(SysCodeTypeDto::getTypeName)).collect(Collectors.toList());
        collect.forEach(p -> p.setExtendOwner(p.isExtendOwner() && TenantContextHolder.getTenantId() != null));
        return collect;
    }

}
