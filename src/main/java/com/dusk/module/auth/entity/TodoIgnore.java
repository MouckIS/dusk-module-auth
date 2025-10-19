package com.dusk.module.auth.entity;

import com.dusk.common.core.entity.FullAuditedEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * @author kefuming
 * @date 2021-12-01 9:18
 */
@Entity
@Table(name = "sys_todo_ignore")
@Getter
@Setter
@FieldNameConstants
public class TodoIgnore extends FullAuditedEntity {
    private Long todoId;
}
