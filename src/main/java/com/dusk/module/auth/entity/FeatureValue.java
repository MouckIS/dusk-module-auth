package com.dusk.module.auth.entity;

import lombok.Data;
import com.dusk.common.core.constant.EntityConstant;
import com.dusk.common.core.entity.CreationEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Version;
import java.time.LocalDateTime;

/**
 * @author kefuming
 * @version 0.0.1
 * @date 2020/4/29 14:20
 */
@Entity
@Table(name = "sys_feature_value")
@Data
public class FeatureValue extends CreationEntity {
    private static final long serialVersionUID = 7849706988482664358L;

    /**
     * 特性名称
     */
    private String name;

    /**
     * 特性值
     */
    private String value;

    /**
     * 特性描述
     */
    private String description;

    private Long editionId;

    @Column(name = EntityConstant.LAST_MODIFY_ID)
    private Long lastModifyId;

    @Column(name = EntityConstant.LAST_MODIFY_TIME)
    private LocalDateTime lastModifyTime;

    @Version
    @Column(name = EntityConstant.VERSION)
    private int version;

    @Column(name = EntityConstant.TENANT_ID)
    private Long tenantId;
}
