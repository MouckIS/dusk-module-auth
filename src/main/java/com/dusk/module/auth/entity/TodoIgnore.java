package com.dusk.module.auth.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import com.dusk.common.framework.entity.FullAuditedEntity;

import javax.persistence.Entity;
import javax.persistence.Table;

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
