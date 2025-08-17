package com.dusk.module.auth.service;

import com.dusk.common.framework.dto.PagedResultDto;
import com.dusk.common.framework.service.IBaseService;
import com.dusk.common.module.auth.dto.dynamicmenu.PublishDynamicMenuInput;
import com.dusk.common.module.auth.service.IDynamicMenuRpcService;
import com.dusk.module.auth.dto.configuration.DynamicMenuDto;
import com.dusk.module.auth.dto.dynamicmenu.DynamicMenuRolesDto;
import com.dusk.module.auth.dto.dynamicmenu.GetDynamicMenuInput;
import com.dusk.module.auth.entity.DynamicMenu;
import com.dusk.module.auth.repository.IDynamicMenuRepository;

import java.util.List;

/**
 * @author kefuming
 * @date 2022-08-29 16:24
 */
public interface IDynamicMenuService extends IBaseService<DynamicMenu, IDynamicMenuRepository>, IDynamicMenuRpcService {

    /**
     * 新增或保存
     * @param input
     * @return
     */
    PagedResultDto<DynamicMenuRolesDto> getList(GetDynamicMenuInput input);

    /**
     * 根据角色列表获取动态菜单
     *
     * @param roleIds
     * @return
     */
    List<DynamicMenuDto> getDynamicMenus(List<Long> roleIds);

    /**
     * 更改发布
     * @param input
     */
    void editPublish(PublishDynamicMenuInput input);

}
