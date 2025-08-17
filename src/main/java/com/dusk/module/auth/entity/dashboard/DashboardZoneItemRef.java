package com.dusk.module.auth.entity.dashboard;

import lombok.Getter;
import lombok.Setter;
import com.dusk.common.framework.entity.BaseEntity;
import com.dusk.common.framework.entity.FullAuditedEntity;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author jianjianhong
 * @date 2021/8/6
 * 区域统计项关联
 */
@Entity
@Table(name = "dashboard_zone_item_ref")
@Getter
@Setter
public class DashboardZoneItemRef extends BaseEntity {
    /**
     * 模块Id
     */
    private Long moduleId;

    /**
     * 统计项ID
     */
    private Long moduleItemId;

    /**
     * 区域ID
     */
    private Long zoneId;

    /**
     * 顺序
     */
    private Integer seq;

    /**
     * 图表颜色
     */
    private String chartColor;

    /**
     * 接线图id
     */
    private Long graphId;
}
