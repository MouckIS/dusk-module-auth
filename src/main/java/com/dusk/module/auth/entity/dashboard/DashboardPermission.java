package com.dusk.module.auth.entity.dashboard;

import lombok.Getter;
import lombok.Setter;
import com.dusk.common.framework.entity.FullAuditedEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * @author jianjianhong
 * @date 2021/7/23
 * 仪表盘权限表
 */
@Entity
@Table(name = "dashboard_premission")
@Getter
@Setter
public class DashboardPermission extends FullAuditedEntity {
    /**
     * 主题Id
     */
    private Long themeId;
    /**
     * 用户Id
     */
    private Long roleId;

    public DashboardPermission() {
    }

    public DashboardPermission(Long themeId, Long roleId) {
        this.themeId = themeId;
        this.roleId = roleId;
    }
}
