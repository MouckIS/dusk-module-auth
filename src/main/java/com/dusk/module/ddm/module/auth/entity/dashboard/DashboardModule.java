package com.dusk.module.ddm.module.auth.entity.dashboard;

import lombok.Getter;
import lombok.Setter;
import com.dusk.common.core.entity.FullAuditedEntity;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author jianjianhong
 * @date 2021/7/23
 * 仪表盘模块
 */
@Entity
@Table(name = "dashboard_module")
@Getter
@Setter
public class DashboardModule extends FullAuditedEntity {
    /**
     * 名称
     */
    private String name;

    /**
     * code
     */
    private String code;

    /**
     * 是否中心模块
     */
    private Boolean centerModule = false;
}
