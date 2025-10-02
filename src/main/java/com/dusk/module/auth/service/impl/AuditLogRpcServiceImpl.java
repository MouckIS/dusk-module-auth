package com.dusk.module.auth.service.impl;

import com.dusk.common.rpc.auth.service.IAuditLogRpcService;
import com.dusk.common.core.dto.AuditLogDto;
import com.github.dozermapper.core.Mapper;
import org.apache.dubbo.config.annotation.Service;
import com.dusk.module.auth.entity.AuditLog;
import com.dusk.module.auth.repository.IAuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author kefuming
 * @date 2020-07-24 15:07
 */
@Service(retries = 0, timeout = 2000)
public class AuditLogRpcServiceImpl implements IAuditLogRpcService {
    @Autowired
    Mapper dozerMapper;
    @Autowired
    IAuditLogRepository repository;

    @Override
    public void saveLog(AuditLogDto log) {
        AuditLog auditLog = dozerMapper.map(log, AuditLog.class);
        repository.save(auditLog);
    }
}
