package com.dusk.module.auth.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import com.dusk.common.framework.annotation.Authorize;
import com.dusk.common.framework.auditlog.DisableAuditLog;
import com.dusk.common.framework.controller.CruxBaseController;
import com.dusk.common.framework.dto.PagedResultDto;
import com.dusk.module.auth.authorization.AuditLogAuthProvider;
import com.dusk.module.auth.dto.auditlog.AuditLogDetailDto;
import com.dusk.module.auth.dto.auditlog.AuditLogListDto;
import com.dusk.module.auth.dto.auditlog.ExportAuditLogsInput;
import com.dusk.module.auth.dto.auditlog.GetAuditLogsInput;
import com.dusk.module.auth.service.IAuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * @author kefuming
 * @date 2020-05-15 13:33
 */
@RestController
@RequestMapping("auditLog")
@Api(tags = "AuditLog", description = "审计日志")
@DisableAuditLog
public class AuditLogController extends CruxBaseController {
    @Autowired
    private IAuditLogService auditLogService;

    @GetMapping("getAuditLogs")
    @ApiOperation("查询审计日志")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "minExecutionDuration", value = "最小持续时间", defaultValue = ""),
            @ApiImplicitParam(name = "maxExecutionDuration", value = "最大持续时间", defaultValue = "")
    })
    @Authorize(AuditLogAuthProvider.PAGES_ADMINISTRATION_AUDITLOGS)
    public PagedResultDto<AuditLogListDto> getAuditLogs(GetAuditLogsInput input) {
        Page<AuditLogListDto> page = auditLogService.findAuditLogs(input);
        return new PagedResultDto<>(page.getTotalElements(), page.getContent());
    }

    @GetMapping("/{id}")
    @ApiOperation("审计日志详情")
    @Authorize(AuditLogAuthProvider.PAGES_ADMINISTRATION_AUDITLOGS)
    public AuditLogDetailDto detail(@PathVariable Long id) {
        return auditLogService.getAuditLogDetail(id);
    }


    @ApiOperation(value = "导出审计日志")
    @GetMapping("exportLog")
    @Authorize(AuditLogAuthProvider.PAGES_ADMINISTRATION_AUDITLOGS_EXPORT)
    public void exportLog(@Valid ExportAuditLogsInput input, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.ms-excel;charset=utf-8");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("审计日志导出", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        auditLogService.exportLog(input, response.getOutputStream());
    }
}
