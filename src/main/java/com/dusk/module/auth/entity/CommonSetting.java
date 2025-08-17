package com.dusk.module.auth.entity;

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import com.dusk.common.framework.entity.FullAuditedEntity;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author kefuming
 * @date 2020-05-18 10:49
 */
@Entity
@Table(name = "sys_common_setting")
@Data
@FieldNameConstants
public class CommonSetting extends FullAuditedEntity {
    /**
     * key
     */
    private String key;

    /**
     * value
     */
    private String value;

    /**
     * 分组名
     */
    private String groupName;

    /**
     * 描述
     */
    private String description;

}
