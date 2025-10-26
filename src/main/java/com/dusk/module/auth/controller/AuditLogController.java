package com.dusk.module.auth.controller;

import com.dusk.common.core.annotation.Authorize;
import com.dusk.common.core.auditlog.DisableAuditLog;
import com.dusk.common.core.controller.CruxBaseController;
import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.module.auth.authorization.AuditLogAuthProvider;
import com.dusk.module.auth.dto.auditlog.AuditLogDetailDto;
import com.dusk.module.auth.dto.auditlog.AuditLogListDto;
import com.dusk.module.auth.dto.auditlog.ExportAuditLogsInput;
import com.dusk.module.auth.dto.auditlog.GetAuditLogsInput;
import com.dusk.module.auth.service.IAuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;

/**
 * @author kefuming
 * @date 2020-05-15 13:33
 */
@RestController
@RequestMapping("auditLog")
@Tag(name = "AuditLog", description = "审计日志")
@DisableAuditLog
public class AuditLogController extends CruxBaseController {
    @Resource
    private IAuditLogService auditLogService;

    @GetMapping("getAuditLogs")
    @Operation(summary = "查询审计日志")
    @Parameters({
            @Parameter(name = "minExecutionDuration", description = "最小持续时间"),
            @Parameter(name = "maxExecutionDuration", description = "最大持续时间")
    })
    @Authorize(AuditLogAuthProvider.PAGES_ADMINISTRATION_AUDITLOGS)
    public PagedResultDto<AuditLogListDto> getAuditLogs(GetAuditLogsInput input) {
        Page<AuditLogListDto> page = auditLogService.findAuditLogs(input);
        return new PagedResultDto<>(page.getTotalElements(), page.getContent());
    }

    @GetMapping("/{id}")
    @Operation(summary = "审计日志详情")
    @Authorize(AuditLogAuthProvider.PAGES_ADMINISTRATION_AUDITLOGS)
    public AuditLogDetailDto detail(@PathVariable Long id) {
        return auditLogService.getAuditLogDetail(id);
    }


    @Operation(summary = "导出审计日志")
    @GetMapping("exportLog")
    @Authorize(AuditLogAuthProvider.PAGES_ADMINISTRATION_AUDITLOGS_EXPORT)
    public void exportLog(@Valid ExportAuditLogsInput input, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.ms-excel;charset=utf-8");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("审计日志导出", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        auditLogService.exportLog(input, response.getOutputStream());
    }
}
