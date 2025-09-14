package com.dusk.module.auth.controller;

import com.dusk.module.auth.dto.dashboard.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import com.dusk.common.framework.annotation.Authorize;
import com.dusk.common.framework.controller.CruxBaseController;
import com.dusk.common.framework.dto.PagedResultDto;
import com.dusk.module.auth.authorization.DashBoardAuthProvider;
import com.dusk.module.auth.dto.dashboard.*;
import com.dusk.module.auth.entity.dashboard.DashboardModule;
import com.dusk.module.auth.entity.dashboard.DashboardModuleItem;
import com.dusk.module.auth.service.IDashBoardModuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;

/**
 * @author jianjianhong
 * @date 2021/7/26
 */
@RestController
@RequestMapping("dashBoard")
@Api(tags = "DashBoard", description = "数据仪表盘")
@Authorize(DashBoardAuthProvider.PAGES_DASHBOARD_THEME_MODULE)
public class DashBoardModuleController extends CruxBaseController {
    @Autowired
    private IDashBoardModuleService moduleService;

    /********************模块接口*************************/

    @PostMapping("/saveModule")
    @ApiOperation("新增或更新模块")
    @Authorize(DashBoardAuthProvider.PAGES_DASHBOARD_THEME_MODULE_EDIT)
    public DashboardModule saveModule(@Validated @RequestBody CreateOrUpdateModule input) {
        return moduleService.saveModule(input);
    }

    @PostMapping("/copyItem")
    @ApiOperation("拷贝模块")
    public void copyItem(@Validated @RequestBody CopyModuleItemInput input) {
        moduleService.copyItem(input);
    }

    @PostMapping("/copyModuleItems")
    @ApiOperation(value = "拷贝模块所有统计项")
    public void copyModuleItems(@Validated @RequestBody CopyModuleItemsInput input) {
        moduleService.copyModuleItems(input);
    }

    @GetMapping("moduleDetail/{id}")
    @ApiOperation(value = "获取模块详细信息")
    public ModuleDetailDto moduleDetail(@PathVariable Long id) {
        return moduleService.moduleDetail(id);
    }

    @DeleteMapping("removeModule/{id}")
    @ApiOperation(value = "删除模块")
    @Authorize(DashBoardAuthProvider.PAGES_DASHBOARD_THEME_MODULE_DELETE)
    public void removeModule(@PathVariable Long id) {
        moduleService.deleteModule(id);
    }

    @GetMapping("/getModuleList")
    @ApiOperation("获取统计模块列表")
    public PagedResultDto<ModuleListDto> getModuleList(@Validated GetModuleInput input) {
        return moduleService.getModuleList(input);
    }

    /********************模块统计项接口*************************/

    @PostMapping("/saveModuleItem")
    @ApiOperation("新增或更新统计项")
    @Authorize(DashBoardAuthProvider.PAGES_DASHBOARD_THEME_MODULE_EDIT)
    public DashboardModuleItem saveModuleItem(@Validated @RequestBody CreateOrUpdateModuleItem input) {
        return moduleService.saveModuleItem(input);
    }

    @DeleteMapping("removeModuleItem/{id}")
    @ApiOperation(value = "删除统计项")
    @Authorize(DashBoardAuthProvider.PAGES_DASHBOARD_THEME_MODULE_EDIT)
    public void removeModuleItem(@PathVariable Long id) {
        moduleService.removeModuleItem(id);
    }

    @ApiOperation("导出模块配置【文本格式】")
    @PostMapping(value = "/exportModule")
    @Authorize(DashBoardAuthProvider.PAGES_DASHBOARD_THEME_MODULE_EXPORT)
    public void exportModule(HttpServletResponse response) {
        moduleService.exportModule(response);
    }

    @ApiOperation("导入模块配置【文本格式】")
    @PostMapping(value = "/importModule")
    @Authorize(DashBoardAuthProvider.PAGES_DASHBOARD_THEME_MODULE_IMPORT)
    public void importModule(ModuleItemPermissionInput permissionInput, @RequestParam("uploadFile") MultipartFile uploadFile) {
        moduleService.importModule(uploadFile, permissionInput);
    }
}
