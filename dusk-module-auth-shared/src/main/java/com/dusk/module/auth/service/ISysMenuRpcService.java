package com.dusk.module.auth.service;

import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.module.auth.dto.sysmenu.GetSysMenuListDto;
import com.dusk.module.auth.dto.sysmenu.GetSysMenuListSearchDto;
import com.dusk.module.auth.dto.sysmenu.SysMenuInputDto;


/**
 * @author 潘彦霖
 * @date 2021-03-19 8:25
 */
public interface ISysMenuRpcService {
    Long save(SysMenuInputDto sysMenuInputDto);

    PagedResultDto<GetSysMenuListDto> list(GetSysMenuListSearchDto map);

    void deleteByIds(java.util.List<Long> collect);
}
