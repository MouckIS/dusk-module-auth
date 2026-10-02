package com.dusk.module.auth.dto;

import cn.hutool.core.text.CharSequenceUtil;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author 王吉
 * @date 2020-07-24 14:59
 */
@Getter
@Setter
public class AuditLogDto implements Serializable {
    private String browserInfo;
    private String clientIpAddress;
    private String exception;
    private int executionDuration;
    private LocalDateTime executionTime;
    private String methodName;
    private String serviceName;
    private Long createId;
    private Long tenantId;
    private String parameters;
    private String orgId;
    //返回值
    private String result;
    // 客户端信息，信息包含客户端的类型、操作系统版本等信息
    private String clientInfo;


    public String toString() {
        String loggedUserId = createId != null
                ? "user " + createId
                : "an anonymous user";

        String exceptionOrSuccessMessage = !CharSequenceUtil.isEmpty(exception)
                ? "exception: " + exception
                : "succeed";

        return String.format("AUDIT LOG: %s.%s is executed by %s in %s ms from %s IP address with %s.", serviceName, methodName, loggedUserId, executionDuration, clientIpAddress, exceptionOrSuccessMessage);
    }
}

