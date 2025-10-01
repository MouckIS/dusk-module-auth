package com.dusk.module.auth.dto.auditlog;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Getter;
import lombok.Setter;
import com.dusk.common.core.jpa.querydsl.QBeanMapper;
import com.dusk.module.auth.entity.AuditLog;
import com.dusk.module.auth.entity.QAuditLog;
import com.dusk.module.auth.entity.QUser;

/**
 * @author kefuming
 * @date 2021-08-09 11:59
 */
@Getter
@Setter
public class AuditLogExportDto {
    @ExcelProperty("执行时间")
    public String time;

    @ExcelIgnore
    @QBeanMapper(target = QAuditLog.class,field = AuditLog.Fields.createId)
    public Long userId;
    @QBeanMapper(target = QUser.class)
    @ExcelProperty("用户")
    public String userName;

    @ExcelProperty("服务")
    public String serviceName;

    @ExcelProperty("操作")
    public String methodName;

    @ExcelProperty("持续时间（ms）")
    public int executionDuration;

    @ExcelProperty("ip地址")
    public String clientIpAddress;

    @ExcelProperty("客户端")
    public String clientName;

    @ExcelProperty("浏览器")
    public String browserInfo;

    @ExcelProperty("调用参数")
    public String parameters;

    @ExcelProperty("异常信息")
    public String exception;
}
