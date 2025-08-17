package com.dusk.module.auth.entity;

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import com.dusk.common.framework.annotation.Tenant;
import com.dusk.common.framework.constant.EntityConstant;
import com.dusk.common.framework.entity.BaseEntity;
import com.dusk.module.auth.enums.LoginLogType;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * @author kefuming
 * @description: TODO
 * @date 2022/10/28
 */
@Entity
@Table(name = "sys_user_login_log")
@Data
@FieldNameConstants
public class UserLoginLog extends BaseEntity {
    @Tenant
    @Column(name = EntityConstant.TENANT_ID)
    private Long tenantId;

    /**
     * 用户名
     */
    private String userName;
    /**
     * 登录日志类型
     */
    @Enumerated(EnumType.STRING)
    private LoginLogType logType;
    /**
     * 操作时间
     */
    private LocalDateTime operationTime;
    /**
     * ip
     */
    private String ip;
    /**
     * 浏览器信息
     */
    private String browserInfo;

    private boolean success;

    private String msg;
}
