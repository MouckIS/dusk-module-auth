package com.dusk.module.auth.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import com.dusk.common.framework.entity.FullAuditedEntity;
import com.dusk.common.module.auth.enums.ToDoTargetType;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @author kefuming
 * @date 2020-08-04 11:24
 */
@Entity
@Table(name = "sys_todos")
@Getter
@Setter
@FieldNameConstants
@NamedEntityGraph(name = "todo.permissions", attributeNodes = {
        @NamedAttributeNode(value = "todoPermissions"),
})
public class Todo extends FullAuditedEntity {
    //类型 code
    @Column(length = 50)
    private String type;
    //类型 名称 用于前端显示
    @Column(length = 50)
    private String typeName;
    //待办标题，不要超过255字符
    private String title;

    //业务状态位
    private String state;

    @Enumerated(EnumType.STRING)
    private ToDoTargetType targetType;

    //是否已完成
    private boolean finish;
    //完成人id
    private Long finishUseId;

    //完成时间
    private LocalDateTime finishTime;

    //关联业务id
    @Column(length = 50)
    private String businessId;

    //拓展字段
    @Column(length = 2000)
    private String extensions;

    /**
     * 场站过滤 为空则是全部都可以读取到
     */
    private Long orgId;

    /**
     * 发起人名字
     */
    private String starter;

    /**
     * 上一提交人
     */
    private String preHandler;

    //子类型
    @Column(length = 50)
    private String subType;

    @Column(length = 50)
    private String subBusinessId;

    @OneToMany(targetEntity = TodoPermission.class, mappedBy = TodoPermission.Fields.todo, cascade = CascadeType.ALL)
    private List<TodoPermission> todoPermissions = new ArrayList<>();

    public void addPermission(String permission) {
        TodoPermission data = new TodoPermission();
        data.setTodo(this);
        data.setPermission(permission);
        todoPermissions.add(data);
    }
}
