package com.dusk.module.auth.entity;

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import com.dusk.common.framework.constant.EntityConstant;
import com.dusk.common.framework.entity.CreationEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Version;
import java.time.LocalDateTime;

/**
 * @author kefuming
 * @date 2020-05-21 13:33
 */
@Entity
@Table(name = "sys_setting")
@Data
@FieldNameConstants
public class Setting extends CreationEntity {
    @Column(name = EntityConstant.LAST_MODIFY_ID)
    private Long lastModifyId;

    @Column(name = EntityConstant.LAST_MODIFY_TIME)
    private LocalDateTime lastModifyTime;

    @Version
    @Column(name = EntityConstant.VERSION)
    private int version;

    @Column(name = EntityConstant.TENANT_ID)
    private Long tenantId;

    /**
     * 厂站id
     */
    private Long stationId;

    private Long userId;

    private String name;

    private String value;
}
