package com.dusk.module.auth.dto.auditlog;

import io.swagger.annotations.ApiModelProperty;
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
    @ApiModelProperty("开始时间")
    @NotNull(message = "开始时间不能为空")
    public LocalDateTime startDate;
    @ApiModelProperty("结束时间")
    @NotNull(message = "结束时间不能为空")
    public LocalDateTime endDate;
    @ApiModelProperty("用户名")
    public String userName;
    @ApiModelProperty("服务")
    public String serviceName;
    @ApiModelProperty("操作")
    public String methodName;
    @ApiModelProperty("浏览器")
    public String browserInfo;
    @ApiModelProperty("错误状态")
    public Boolean hasException;
}
