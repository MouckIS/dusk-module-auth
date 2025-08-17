package com.dusk.module.auth.entity;

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import com.dusk.common.framework.entity.FullAuditedEntity;
import com.dusk.common.module.auth.enums.EnumResetType;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * @author kefuming
 * @date 2020-09-22 14:35
 */
@Entity
@Table(name = "sys_serial_no")
@Data
@FieldNameConstants
public class SerialNo extends FullAuditedEntity {
    /**
     * 单据类型
     */
    private String billType;
    /**
     * 重置类型
     */
    @Enumerated(EnumType.STRING)
    private EnumResetType resetType;
    /**
     * 当前序号
     */
    private long currentNo;
    /**
     * 最后一次的序列号
     */
    private String lastNo;

    /**
     * 日期格式化
     */
    private String dateFormat;

    /**
     * 序列化长度 例如 0001 4位
     */
    private int noLength;

    private LocalDateTime lastUpdateTime;
}
