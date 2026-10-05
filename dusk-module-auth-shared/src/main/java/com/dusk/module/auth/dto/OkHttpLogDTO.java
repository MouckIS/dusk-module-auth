package com.dusk.module.auth.dto;

import com.dusk.common.core.dto.EntityDto;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * @Author 李思
 * @CreateTime 2023/8/21 14:54
 */
@Getter
@Setter
public class OkHttpLogDTO extends EntityDto {

    private String url;

    private String method;

    private String headers;

    private String requestParams;

    private String requestBody;

    private Integer code;

    private String protocol;

    private String response;

    private String exception;

    private int executionDuration;

    private LocalDateTime executionTime;

    private String className;

    private String methodName;


}
