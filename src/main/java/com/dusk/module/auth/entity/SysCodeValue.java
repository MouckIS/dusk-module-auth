package com.dusk.module.auth.entity;

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import com.dusk.common.framework.entity.FullAuditedEntity;

import javax.persistence.Entity;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "sys_code_values")
@FieldNameConstants
public class SysCodeValue extends FullAuditedEntity {
    /**
     * 配置常量所属类型id
     */
    private String typeCode;
    /**
     * 配置常量key
     */
    private String code;
    /**
     * 配置常量值
     */
    private String value;
    /**
     * 配置常量描述
     */
    private String describe;
    /**
     * 配置常量在所属类型组中排序
     */
    private int sortIndex;
    /**
     * 父节点ID
     */
    private Long parentId;

}
