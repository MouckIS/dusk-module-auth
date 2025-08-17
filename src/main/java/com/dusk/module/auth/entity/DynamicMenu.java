package com.dusk.module.auth.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import com.dusk.common.framework.annotation.Tenant;
import com.dusk.common.framework.constant.EntityConstant;
import com.dusk.common.framework.entity.CreationEntity;
import com.dusk.common.module.auth.enums.DynamicMenuOpenType;

import javax.persistence.*;

/**
 * @author kefuming
 * @date 2022-08-29 16:00
 */
@Entity
@Table(name = "sys_dynamic_menu")
@Getter
@Setter
@FieldNameConstants
public class DynamicMenu extends CreationEntity {
    //菜单名
    private String name;
    //类型 前端用于判断  常见 动态报表、低代码、其他 这边不定义枚举得原因是因为对于auth来说无意义
    private String dynamicType;
    //标识 前端根据这个字段以及类型动态组装路由，第三方得应该直接跳转即可
    private String identify;
    //来源key 唯一标识 用于区分菜单从哪里来的 这里搞个索引
    private String businessKey;
    //角色id
    private Long roleId;

    @Tenant
    @Column(name = EntityConstant.TENANT_ID)
    private Long tenantId;

    @Enumerated(EnumType.STRING)
    private DynamicMenuOpenType type;

}
