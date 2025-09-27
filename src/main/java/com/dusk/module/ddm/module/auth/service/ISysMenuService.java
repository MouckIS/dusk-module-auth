package com.dusk.module.ddm.module.auth.service;


import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.common.core.service.IBaseService;
import com.dusk.common.module.auth.dto.sysmenu.GetSysMenuListDto;
import com.dusk.common.module.auth.dto.sysmenu.GetSysMenuListSearchDto;
import com.dusk.common.module.auth.dto.sysmenu.SysMenuInputDto;
import com.dusk.module.auth.entity.SysMenu;
import com.dusk.module.auth.repository.ISysMenuRepository;

import java.util.List;

public interface ISysMenuService extends IBaseService<SysMenu, ISysMenuRepository> {

   void save(List<SysMenuInputDto> input);

   Long save(SysMenuInputDto input);

   void delete(Long id);

   /**
    * 根据id删除(不包括子节点)
    * @param id
    */
   void deleteCurrent(Long id);

   PagedResultDto<GetSysMenuListDto> list(GetSysMenuListSearchDto input);

   void deleteByIds(List<Long> ids);
}
