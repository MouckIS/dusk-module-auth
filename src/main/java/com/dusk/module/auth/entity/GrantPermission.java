package com.dusk.module.auth.entity;

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import com.dusk.common.core.annotation.Tenant;
import com.dusk.common.core.constant.EntityConstant;
import com.dusk.common.core.entity.BaseEntity;
import com.dusk.common.core.entity.CreationEntity;

import javax.persistence.*;

/**
 * @author kefuming
 * @date 2020-04-29 10:50
 */
@Entity
@Table(name = "sys_permissions")
@Data
@FieldNameConstants
@NamedEntityGraph(name = "GrantPermission.includeAll", attributeNodes = {@NamedAttributeNode("role")})
public class GrantPermission extends CreationEntity {
    private static final long serialVersionUID = -54289370934471233L;

    //private String roleId;
    /**
     * 权限的name
     */
    private String name;

    @Tenant
    @Column(name = EntityConstant.TENANT_ID)
    private Long tenantId;

    @ManyToOne(targetEntity = Role.class, optional = false)
    @JoinColumn(name = "role_id", referencedColumnName = BaseEntity.Fields.id)
    private Role role;

    /**
     * 业务来源 仅仅针对动态功能
     */
    private String businessKey;
}
