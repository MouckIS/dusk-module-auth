package com.dusk.module.auth.service;

import com.dusk.common.framework.service.IBaseService;
import com.dusk.module.auth.dto.auditlog.AuditLogDetailDto;
import com.dusk.module.auth.dto.auditlog.AuditLogListDto;
import com.dusk.module.auth.dto.auditlog.ExportAuditLogsInput;
import com.dusk.module.auth.dto.auditlog.GetAuditLogsInput;
import com.dusk.module.auth.entity.AuditLog;
import com.dusk.module.auth.repository.IAuditLogRepository;
import org.springframework.data.domain.Page;

import javax.servlet.ServletOutputStream;

/**
 * @author kefuming
 * @date 2020-05-15 11:49
 */
public interface IAuditLogService extends IBaseService<AuditLog, IAuditLogRepository> {
    /**
     * 查询审计日志
     * @param input
     * @return
     */
    Page<AuditLogListDto> findAuditLogs(GetAuditLogsInput input);

    AuditLogDetailDto getAuditLogDetail(Long id);

    /**
     * 导出审计日志
     * @param input
     * @param outputStream
     */
    void exportLog(ExportAuditLogsInput input, ServletOutputStream outputStream);
}
