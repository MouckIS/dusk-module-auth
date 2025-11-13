package com.dusk.module.auth.dto.auditlog;

import com.dusk.common.core.dto.EntityDto;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import java.time.LocalDateTime;

/**
 * @author kefuming
 * @date 2020-05-15 11:38
 */
@Getter
@Setter
@FieldNameConstants
public class AuditLogListDto extends EntityDto {
    //@QBeanMapper(target = QAuditLog.class,field = AuditLog.Fields.createId)
    public Long userId;
    //@QBeanMapper(target = QUser.class)
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
