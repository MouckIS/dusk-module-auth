package com.dusk.module.auth.entity;


import lombok.Data;
import lombok.experimental.FieldNameConstants;
import com.dusk.common.core.annotation.Tenant;
import com.dusk.common.core.constant.EntityConstant;
import com.dusk.common.core.entity.CreationEntity;
import com.dusk.common.module.auth.enums.MenuTypeEnum;

import javax.persistence.*;

@Data
@Entity
@FieldNameConstants
@Table(name="sys_menu")
public class SysMenu extends CreationEntity {
    /**
     * 前端路由的title
     */
    private  String title;

    /**
     * 前端路由的name
     */
    private  String routeName;

    /**
     * 前端路由的icon类型
     */
    private  String iconType;

    /**
     * 前端路由的icon的class
     */
    private  String iconClass;

    /**
     * 排序
     */
    private Integer sortIndex;

    /**
     * 父级name
     */
    private String parentRouteName;

    @Tenant
    @Column(name = EntityConstant.TENANT_ID,updatable = false)
    private Long tenantId;

    private Boolean ignored;

    /**
     * 菜单类型
     */
    @Enumerated(EnumType.STRING)
    private MenuTypeEnum type;
}
