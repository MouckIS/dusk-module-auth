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
 * 维度值实体
 *
 * @author dusk
 */
@Getter
@Setter
@Entity
@Table(name = "sys_dimension_value", uniqueConstraints = {
        @UniqueConstraint(name = "uk_dim_value_code_tenant", columnNames = {"dimension_id", "value_code", "tenant_id"})
})
@FieldNameConstants
public class DimensionValue extends FullAuditedEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 所属维度ID
     */
    @Column(name = "dimension_id", nullable = false)
    private Long dimensionId;

    /**
     * 维度值编码
     */
    @Column(name = "value_code", nullable = false, length = 100)
    private String valueCode;

    /**
     * 维度值名称
     */
    @Column(name = "value_name", nullable = false, length = 200)
    private String valueName;

    /**
     * 维度值描述
     */
    @Column(name = "value_desc", length = 500)
    private String valueDesc;

    /**
     * 排序号
     */
    @Column(name = "sort_index")
    private Integer sortIndex;

    /**
     * 是否启用
     */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;
}
