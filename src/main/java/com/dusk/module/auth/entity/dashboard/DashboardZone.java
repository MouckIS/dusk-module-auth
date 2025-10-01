package com.dusk.module.auth.entity.dashboard;

import lombok.Getter;
import lombok.Setter;
import com.dusk.common.core.entity.FullAuditedEntity;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author jianjianhong
 * @date 2021/8/6
 * 区域
 */
@Entity
@Table(name = "dashboard_zone")
@Getter
@Setter
public class DashboardZone extends FullAuditedEntity {
    /**
     * 名称
     */
    private String name;

    /**
     * 栏目ID
     */
    private Long classifyId;

    /**
     * 布局方向
     */
    private String orientation;

    /**
     * 位置
     */
    private Integer zonePosition;
}
