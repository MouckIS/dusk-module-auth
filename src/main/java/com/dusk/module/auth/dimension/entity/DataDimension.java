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
 * 数据维度实体
 *
 * @author dusk
 */
@Getter
@Setter
@Entity
@Table(name = "sys_data_dimension", uniqueConstraints = {
        @UniqueConstraint(name = "uk_dimension_code_tenant", columnNames = {"dimension_code", "tenant_id"})
})
@FieldNameConstants
public class DataDimension extends FullAuditedEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 维度名称
     */
    @Column(name = "dimension_name", nullable = false, length = 100)
    private String dimensionName;

    /**
     * 维度编码（不可修改）
     */
    @Column(name = "dimension_code", nullable = false, length = 100, updatable = false)
    private String dimensionCode;

    /**
     * 维度描述
     */
    @Column(name = "dimension_desc", length = 500)
    private String dimensionDesc;

    /**
     * 是否启用
     */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;
}
