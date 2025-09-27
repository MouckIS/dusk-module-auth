package com.dusk.module.auth.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import com.dusk.common.core.entity.BaseEntity;
import com.dusk.common.core.entity.CreationEntity;

import javax.persistence.*;

/**
 * @author kefuming
 * @date 2020-08-05 10:35
 */
@Entity
@Table(name = "sys_todo_permissions")
@Getter
@Setter
@FieldNameConstants
public class TodoPermission extends CreationEntity {
    private String permission;

    @Column(insertable = false,updatable = false)
    private Long todoId;

    @ManyToOne(targetEntity = Todo.class, optional = false)
    @JoinColumn(name = Fields.todoId ,referencedColumnName = BaseEntity.Fields.id)
    private Todo todo;
}
