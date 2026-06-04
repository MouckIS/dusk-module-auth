package com.dusk.module.auth.dimension.entity;

import com.dusk.common.core.entity.CreationEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * 数据维度操作日志实体
 *
 * @author dusk
 */
@Getter
@Setter
@Entity
@Table(name = "sys_dimension_operation_log")
@FieldNameConstants
public class DimensionOperationLog extends CreationEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 操作类型
     */
    @Column(name = "operation_type", nullable = false)
    private Integer operationType;

    /**
     * 操作目标类型（DIMENSION/DIMENSION_VALUE/PERMISSION）
     */
    @Column(name = "target_type", nullable = false, length = 50)
    private String targetType;

    /**
     * 操作目标ID
     */
    @Column(name = "target_id")
    private Long targetId;

    /**
     * 操作目标名称
     */
    @Column(name = "target_name", length = 200)
    private String targetName;

    /**
     * 操作详情
     */
    @Column(name = "operation_detail", length = 2000)
    private String operationDetail;

    /**
     * 操作用户ID
     */
    @Column(name = "operator_id")
    private Long operatorId;

    /**
     * 操作用户名称
     */
    @Column(name = "operator_name", length = 100)
    private String operatorName;

    /**
     * 租户ID
     */
    @Column(name = "tenant_id")
    private Long tenantId;

    /**
     * 客户端IP
     */
    @Column(name = "client_ip", length = 50)
    private String clientIp;
}
