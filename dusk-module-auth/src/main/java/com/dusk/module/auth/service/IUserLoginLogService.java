package com.dusk.module.auth.service;

import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.common.core.service.IBaseService;
import com.dusk.module.auth.dto.loginlog.ListUserLoginLogInput;
import com.dusk.module.auth.dto.loginlog.UserLoginLogDto;
import com.dusk.module.auth.entity.UserLoginLog;
import com.dusk.module.auth.listener.LogInOutEvent;
import com.dusk.module.auth.repository.IUserLoginLogRepository;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

/**
 * @author kefuming
 * @description: TODO
 * @date 2022/10/28
 */
public interface IUserLoginLogService extends IBaseService<UserLoginLog, IUserLoginLogRepository> {
    @Async
    @EventListener(LogInOutEvent.class)
    void saveLog(LogInOutEvent event);

    PagedResultDto<UserLoginLogDto> listLog(ListUserLoginLogInput input);
}
