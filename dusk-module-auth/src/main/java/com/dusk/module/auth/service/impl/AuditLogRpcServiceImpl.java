package com.dusk.module.auth.service.impl;

import com.dusk.common.core.dto.AuditLogDto;
import com.dusk.module.auth.service.IAuditLogRpcService;
import com.dusk.module.auth.entity.AuditLog;
import com.dusk.module.auth.repository.IAuditLogRepository;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.beans.BeanUtils;

/**
 * @author kefuming
 * @date 2020-07-24 15:07
 */
@Service
public class AuditLogRpcServiceImpl implements IAuditLogRpcService {
    @Resource
    private IAuditLogRepository repository;

    @Override
    public void saveLog(AuditLogDto log) {
        AuditLog auditLog = new AuditLog();
        BeanUtils.copyProperties(log, auditLog);
        repository.save(auditLog);
    }
}
