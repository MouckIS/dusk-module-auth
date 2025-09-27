package com.dusk.module.ddm.module.auth.service.impl;

import org.apache.dubbo.config.annotation.Service;
import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.common.module.auth.dto.sysmenu.GetSysMenuListDto;
import com.dusk.common.module.auth.dto.sysmenu.GetSysMenuListSearchDto;
import com.dusk.common.module.auth.dto.sysmenu.SysMenuInputDto;
import com.dusk.common.module.auth.service.ISysMenuRpcService;
import com.dusk.module.auth.service.ISysMenuService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * @author kefuming
 * @date 2021-03-19 8:28
 */
@Service
public class SysMenuRpcServiceImpl implements ISysMenuRpcService {
    @Autowired
    ISysMenuService sysMenuService;
    
    @Override
    public Long save(SysMenuInputDto input) {
        return sysMenuService.save(input);
    }

    @Override
    public void deleteByIds(List<Long> ids) {
        sysMenuService.deleteByIds(ids);
    }

    @Override
    public PagedResultDto<GetSysMenuListDto> list(GetSysMenuListSearchDto input) {
        return sysMenuService.list(input);
    }
}
