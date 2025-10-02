package com.dusk.module.auth.entity.dashboard;

import lombok.Getter;
import lombok.Setter;
import com.dusk.common.core.entity.FullAuditedEntity;
import com.dusk.module.auth.enums.dashboard.DashboardModuleType;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

/**
 * @author jianjianhong
 * @date 2021/7/23
 * 业务模块统计项
 */
@Entity
@Table(name = "dashboard_module_item")
@Getter
@Setter
public class DashboardModuleItem extends FullAuditedEntity {
    /**
     * code
     */
    private String code;
    /**
     * 名称
     */
    private String name;
    /**
     * 详情路径
     */
    private String detailPath;
    /**
     * 数据来源
     */
    private String dataSource;

    /**
     * 类型
     */
    @Enumerated(EnumType.STRING)
    private DashboardModuleType moduleType;

    /**
     * 模块Id
     */
    private Long moduleId;

    /**
     * 图表类型
     */
    private String chartType;

}
