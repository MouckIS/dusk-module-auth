package com.dusk.module.auth.dimension.entity;

import com.dusk.common.core.entity.FullAuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;

/**
 * 用户维度值权限实体
 *
 * @author dusk
 */
@Getter
@Setter
@Entity
@Table(name = "sys_user_dimension_permission", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_dim_value_tenant", columnNames = {"user_id", "dimension_value_id", "tenant_id"})
})
@FieldNameConstants
public class UserDimensionPermission extends FullAuditedEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 维度ID
     */
    @Column(name = "dimension_id", nullable = false)
    private Long dimensionId;

    /**
     * 维度值ID
     */
    @Column(name = "dimension_value_id", nullable = false)
    private Long dimensionValueId;
}
