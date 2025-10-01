package com.dusk.module.auth.dto.auditlog;

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import com.dusk.common.core.dto.EntityDto;
import com.dusk.common.core.jpa.querydsl.QBeanMapper;
import com.dusk.module.auth.entity.AuditLog;
import com.dusk.module.auth.entity.QAuditLog;
import com.dusk.module.auth.entity.QUser;

import java.time.LocalDateTime;

/**
 * @author kefuming
 * @date 2020-05-15 11:38
 */
@Data
@FieldNameConstants
public class AuditLogListDto extends EntityDto {
    @QBeanMapper(target = QAuditLog.class,field = AuditLog.Fields.createId)
    public Long userId;
    @QBeanMapper(target = QUser.class)
    public String userName;

    public String serviceName;

    public String methodName;

    public LocalDateTime executionTime;

    public int executionDuration;

    public String clientIpAddress;

    public String clientName;

    public String browserInfo;

    public String customData;

}
