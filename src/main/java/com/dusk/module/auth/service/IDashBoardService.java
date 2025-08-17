package com.dusk.module.auth.service;

import com.dusk.module.auth.dto.dashboard.*;
import com.dusk.common.framework.dto.PagedResultDto;
import com.dusk.common.framework.service.IBaseService;
import com.dusk.common.module.auth.dto.RoleSimpleDto;
import com.dusk.module.auth.dto.dashboard.*;
import com.dusk.module.auth.entity.dashboard.DashboardTheme;
import com.dusk.module.auth.entity.dashboard.DashboardClassify;
import com.dusk.module.auth.repository.dashboard.IDashBoardThemeRepository;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * @author jianjianhong
 * @date 2021-07-21
 */
public interface IDashBoardService extends IBaseService<DashboardTheme, IDashBoardThemeRepository> {

    /**
     * 新增或更新主题
     * @param input
     * @return
     */
    DashboardTheme saveTheme(CreateOrUpdateTheme input);

    /**
     * 获取主题列表
     * @param input
     * @return
     */
    PagedResultDto<ThemeListDto> getThemeList(GetThemeInput input);

    /**
     * 主题详情
     * @param id
     * @return
     */
    ThemeDetailDto themeDetail(Long id);

    /**
     * 新增或更新模块大类
     * @param input
     * @return
     */
    DashboardClassify saveClassify(@Validated @RequestBody CreateOrUpdateClassify input);

    /**
     * 大类详情
     * @param id
     * @return
     */
    ClassifyDetailDto classifyDetail(Long id);

    /**
     * 删除栏目
     * @param id
     */
    void removeClassify(Long id);

    /**
     * 删除主题
     * @param id
     */
    void removeTheme(Long id);

    /**
     * 设置主题权限
     * @param input
     */
    void setDashBardPermission(CreateOrUpdateDashBoardPermission input);

    /**
     * 删除主题权限
     * @param input
     */
    void removeDashBardPermission(RemoveDashBoardPermission input);

    /**
     * 获取用户的主题列表
     * @param userId
     * @return
     */
    List<ThemeListDto> getDashBoardThemeByUserId(Long userId);

    /**
     * 根据主题已授权的角色列表
     * @param themeId
     * @return
     */
    List<RoleSimpleDto> getDashBoardThemeUser(Long themeId);

    /**
     * 获取用户首页大屏
     * @param userId
     * @return
     */
    UserMainDashBoardDto getUserMainDashBoard(Long userId);

    /**
     * 检查首页大屏是否正确
     * @param themeId
     * @return
     */
    Boolean checkMainDashBoard(Long themeId);
}
