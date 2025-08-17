package com.dusk.module.auth.entity.dashboard;

import lombok.Getter;
import lombok.Setter;
import com.dusk.common.framework.entity.FullAuditedEntity;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author jianjianhong
 * @date 2021/7/23
 * 仪表盘主题
 */
@Entity
@Table(name = "dashboard_theme")
@Getter
@Setter
public class DashboardTheme extends FullAuditedEntity {
    /**
     * 主题名称
     */
    private String name;
    /**
     * 主题样式
     */
    private String themeType;

    /**
     * 标题
     */
    private String title;

    /**
     * 描述
     */
    private String description;

    /**
     * 显示时间
     */
    private Boolean showTime;

    /**
     * 显示天气
     */
    private Boolean showWeather;

    /**
     * 是否首页大屏
     */
    private Boolean mainPage;
}
