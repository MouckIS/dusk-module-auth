package com.dusk.module.auth.controller;

import com.dusk.common.core.annotation.Authorize;
import com.dusk.common.core.controller.CruxBaseController;
import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.common.rpc.auth.dto.RoleSimpleDto;
import com.dusk.module.auth.authorization.DashBoardAuthProvider;
import com.dusk.module.auth.dto.dashboard.*;
import com.dusk.module.auth.entity.dashboard.DashboardClassify;
import com.dusk.module.auth.entity.dashboard.DashboardTheme;
import com.dusk.module.auth.service.IDashBoardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author jianjianhong
 * @date 2021/7/26
 */
@RestController
@RequestMapping("dashBoard")
@Tag(name = "DashBoard", description = "数据仪表盘")
@Authorize(DashBoardAuthProvider.PAGES_DASHBOARD_THEME)
public class DashBoardController extends CruxBaseController {
    @Resource
    private IDashBoardService service;

    @PostMapping("/saveTheme")
    @Operation(summary = "新增或更新主题")
    @Authorize(DashBoardAuthProvider.PAGES_DASHBOARD_THEME_EDIT)
    public DashboardTheme saveTheme(@Validated @RequestBody CreateOrUpdateTheme input) {
        return service.saveTheme(input);
    }

    @GetMapping("/getThemeList")
    @Operation(summary = "获取主题列表")
    public PagedResultDto<ThemeListDto> getThemeList(@Validated GetThemeInput input) {
        return service.getThemeList(input);
    }

    @GetMapping("/themeDetail/{id}")
    @Operation(summary = "主题详情")
    public ThemeDetailDto themeDetail(@PathVariable Long id) {
        return service.themeDetail(id);
    }

    @DeleteMapping("/removeTheme/{id}")
    @Operation(summary = "删除主题")
    @Authorize(DashBoardAuthProvider.PAGES_DASHBOARD_THEME_DELETE)
    public void removeTheme(@PathVariable Long id) {
        service.removeTheme(id);
    }

    @PostMapping("/saveClassify")
    @Operation(summary = "新增或更新栏目")
    @Authorize(DashBoardAuthProvider.PAGES_DASHBOARD_THEME_EDIT)
    public DashboardClassify saveClassify(@Validated @RequestBody CreateOrUpdateClassify input) {
        return service.saveClassify(input);
    }

    @GetMapping("/classify/{id}")
    @Operation(summary = "栏目详情")
    public ClassifyDetailDto classifyDetail(@PathVariable Long id) {
        return service.classifyDetail(id);
    }

    @DeleteMapping("/removeClassify/{id}")
    @Operation(summary = "删除栏目")
    @Authorize(DashBoardAuthProvider.PAGES_DASHBOARD_THEME_EDIT)
    public void removeClassify(@PathVariable Long id) {
        service.removeClassify(id);
    }

    @GetMapping("/dashBardPermission")
    @Operation(summary = "设置主题权限")
    @Authorize(DashBoardAuthProvider.PAGES_DASHBOARD_THEME_EDIT)
    public void setDashBardPermission(@Validated CreateOrUpdateDashBoardPermission input) {
        service.setDashBardPermission(input);
    }

    @DeleteMapping("/removeDashBardPermission")
    @Operation(summary = "删除主题权限")
    @Authorize(DashBoardAuthProvider.PAGES_DASHBOARD_THEME_EDIT)
    public void removeDashBardPermission(@Validated RemoveDashBoardPermission input) {
        service.removeDashBardPermission(input);
    }


    @GetMapping("/dashBoardTheme/{userId}")
    @Operation(summary = "根据UserId获取主题列表")
    public List<ThemeListDto> getDashBoardThemeByUserId(@PathVariable Long userId) {
        return service.getDashBoardThemeByUserId(userId);
    }

    @GetMapping("/dashBoardThemeUser/{themeId}")
    @Operation(summary = "根据主题已授权的角色列表")
    public List<RoleSimpleDto> getDashBoardThemeUser(@PathVariable Long themeId) {
        return service.getDashBoardThemeUser(themeId);
    }

    @GetMapping("/mainDashBoard/{userId}")
    @Operation(summary = "获取用户首页大屏")
    public UserMainDashBoardDto getUserMainDashBoard(@PathVariable Long userId) {
        return service.getUserMainDashBoard(userId);
    }

    @GetMapping("/checkMainDashBoard")
    @Operation(summary = "检查首页大屏是否正确")
    public Boolean checkMainDashBoard(@RequestParam(value = "themeId",required = false) Long themeId) {
        return service.checkMainDashBoard(themeId);
    }
}
