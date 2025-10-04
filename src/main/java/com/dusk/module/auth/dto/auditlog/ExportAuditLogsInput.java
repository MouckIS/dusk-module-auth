package com.dusk.module.auth.dto.auditlog;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * @author kefuming
 * @date 2021-08-09 11:30
 */
@Getter
@Setter
public class ExportAuditLogsInput {
    @Schema(description = "开始时间")
    @NotNull(message = "开始时间不能为空")
    public LocalDateTime startDate;
    @Schema(description = "结束时间")
    @NotNull(message = "结束时间不能为空")
    public LocalDateTime endDate;
    @Schema(description = "用户名")
    public String userName;
    @Schema(description = "服务")
    public String serviceName;
    @Schema(description = "操作")
    public String methodName;
    @Schema(description = "浏览器")
    public String browserInfo;
    @Schema(description = "错误状态")
    public Boolean hasException;
}
