package com.dusk.module.ddm.module.auth.listener;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.dusk.module.auth.enums.LoginLogType;

import java.time.LocalDateTime;

/**
 * @author kefuming
 * @description: TODO
 * @date 2022/10/31
 */
@Getter
@Setter
@NoArgsConstructor
public class LogInOutEvent {
    private String userName;
    private LoginLogType logType;
    private LocalDateTime operationTime;
    private String ip;
    private String browserInfo;
    private boolean success;
    private String msg;

    public LogInOutEvent(LoginLogType logType, LocalDateTime operationTime, boolean success) {
        this.logType = logType;
        this.operationTime = operationTime;
        this.success = success;
    }
}
