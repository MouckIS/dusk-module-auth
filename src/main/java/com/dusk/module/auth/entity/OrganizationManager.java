package com.dusk.module.auth.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import com.dusk.common.core.entity.FullAuditedEntity;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author kefuming
 * @CreateTime 2022-11-08
 */
@Entity
@Getter
@Setter
@FieldNameConstants
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "sys_org_manager")
public class OrganizationManager extends FullAuditedEntity {
    /**
     * 组织id
     */
    private Long orgId;
    /**
     * 组织管理员id
     */
    private Long userId;
}
