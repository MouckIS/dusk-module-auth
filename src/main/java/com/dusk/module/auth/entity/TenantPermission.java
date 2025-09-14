package com.dusk.module.auth.entity;

import lombok.Data;
import com.dusk.common.framework.entity.CreationEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 租户和版本权限公用的表
 *
 * @author kefuming
 * @date 2020-12-11 9:07
 */
@Entity
@Table(name = "sys_tenant_permissions")
@Data
public class TenantPermission extends CreationEntity {
    /**
     * 版本id
     */
    private Long editionId;

    /**
     * 权限名称
     */
    private String name;


}
