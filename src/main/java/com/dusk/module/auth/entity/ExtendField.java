package com.dusk.module.auth.entity;

import com.dusk.common.core.entity.FullAuditedEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * @author kefuming
 * @date 2021-11-19 15:10
 */
@Entity
@Table(name = "sys_extend_field")
@Getter
@Setter
@FieldNameConstants
public class ExtendField extends FullAuditedEntity {
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
