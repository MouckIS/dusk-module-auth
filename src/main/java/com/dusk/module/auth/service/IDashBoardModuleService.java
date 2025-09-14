package com.dusk.module.auth.service;

import com.dusk.module.auth.dto.dashboard.*;
import com.dusk.common.framework.dto.PagedResultDto;
import com.dusk.common.framework.service.IBaseService;
import com.dusk.module.auth.dto.dashboard.*;
import com.dusk.module.auth.entity.dashboard.DashboardModule;
import com.dusk.module.auth.entity.dashboard.DashboardModuleItem;
import com.dusk.module.auth.repository.dashboard.IDashBoardModuleRepository;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;

/**
 * @author jianjianhong
 * @date 2021-07-21
 */
public interface IDashBoardModuleService extends IBaseService<DashboardModule, IDashBoardModuleRepository> {

    /**
     * 新增或更新模块
     * @param input
     * @return
     */
    DashboardModule saveModule(CreateOrUpdateModule input);

    /**
     * 拷贝模块
     * @param input
     * @return
     */
    void copyItem(CopyModuleItemInput input);

    /**
     * 拷贝模块统计项
     * @param input
     */
    void copyModuleItems(CopyModuleItemsInput input);

    /**
     * 获取模块列表
     * @param input
     * @return
     */
    PagedResultDto<ModuleListDto> getModuleList(GetModuleInput input);

    /**
     * 模块详情
     * @param id
     * @return
     */
    ModuleDetailDto moduleDetail(Long id);

    /**
     * 删除模块
     * @param id
     */
    void deleteModule(Long id);

    /**
     * 新增或更新模块统计项
     * @param input
     * @return
     */
    DashboardModuleItem saveModuleItem(CreateOrUpdateModuleItem input);

    /**
     * 删除模块统计项
     * @param id
     */
    void removeModuleItem(Long id);

    /**
     * 导出模块配置【文本格式】
     * @param response
     */
    void exportModule(HttpServletResponse response);

    /**
     * 导入模块配置【文本格式】
     * @param uploadFile
     */
    void importModule(MultipartFile uploadFile, ModuleItemPermissionInput permissionInput);
}
