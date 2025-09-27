package com.dusk.module.auth.entity;

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import com.dusk.common.core.entity.FullAuditedEntity;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author kefuming
 * @date 2021-11-19 15:10
 */
@Entity
@Table(name = "sys_extend_field")
@Data
@FieldNameConstants
public class ExtendField  extends FullAuditedEntity {
    /**
     * 实体id
     */
    private Long entityId;
    /**
     * 实体类名称
     */
    private String entityClass;

    private String key;

    private String value;
}
