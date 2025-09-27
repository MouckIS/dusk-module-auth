package com.dusk.module.ddm.module.auth.entity.dashboard;

import lombok.Getter;
import lombok.Setter;
import com.dusk.common.core.entity.FullAuditedEntity;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author jianjianhong
 * @date 2021/7/23
 * 栏目
 */
@Entity
@Table(name = "dashboard_classify")
@Getter
@Setter
public class DashboardClassify extends FullAuditedEntity {
    /**
     * 名称
     */
    private String name;
    /**
     * 布局id
     */
    private String layoutId;

    /**
     * 区域总数
     */
    private int zoneNum;

    /**
     * 主题Id
     */
    private Long themeId;

    /**
     * 顺序
     */
    private Integer seq;
}
