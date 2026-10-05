package com.dusk.module.auth.service;


import com.dusk.common.core.dto.AuditLogDto;

/**
 * @author kefuming
 * @date 2020-07-24 14:58
 */
public interface IAuditLogRpcService {

    /**
     * 推送系统日志
     *
     * @param log
     */
    void saveLog(AuditLogDto log);
}
