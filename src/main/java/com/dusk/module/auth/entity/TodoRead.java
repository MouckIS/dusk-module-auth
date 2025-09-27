package com.dusk.module.auth.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import com.dusk.common.core.entity.CreationEntity;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author 李思
 * @description 用于待办列表已读、未读展示
 * @CreateTime 2022/5/26 16:53
 */
@Entity
@Table(name = "sys_todo_read")
@Getter
@Setter
@FieldNameConstants
public class TodoRead extends CreationEntity {

    private Long userId;

    private Long todoId;

}
