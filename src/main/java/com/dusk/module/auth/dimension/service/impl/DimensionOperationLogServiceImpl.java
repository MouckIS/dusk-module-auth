package com.dusk.module.auth.dimension.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.JakartaServletUtil;
import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.common.core.jpa.Specifications;
import com.dusk.common.core.model.UserContext;
import com.dusk.common.core.service.impl.BaseService;
import com.dusk.common.core.utils.SecurityUtils;
import com.dusk.module.auth.dimension.dto.DimensionOperationLogDto;
import com.dusk.module.auth.dimension.dto.DimensionOperationLogPagedInputDto;
import com.dusk.module.auth.dimension.entity.DimensionOperationLog;
import com.dusk.module.auth.dimension.enums.DimensionOperationType;
import com.dusk.module.auth.dimension.repository.IDimensionOperationLogRepository;
import com.dusk.module.auth.dimension.service.IDimensionOperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 维度操作日志服务实现
 *
 * @author dusk
 */
@Service
public class DimensionOperationLogServiceImpl extends BaseService<DimensionOperationLog, IDimensionOperationLogRepository>
        implements IDimensionOperationLogService {

    @Autowired
    private SecurityUtils securityUtils;

    @Override
    public PagedResultDto<DimensionOperationLogDto> getPage(DimensionOperationLogPagedInputDto input) {
        Page<DimensionOperationLog> page = repository.findAll(
                Specifications.where(w -> {
                    w.eq(input.getOperationType() != null, DimensionOperationLog.Fields.operationType, input.getOperationType());
                    w.eq(StrUtil.isNotBlank(input.getTargetType()), DimensionOperationLog.Fields.targetType, input.getTargetType());
                    w.eq(input.getOperatorId() != null, DimensionOperationLog.Fields.operatorId, input.getOperatorId());
                    w.contains(StrUtil.isNotBlank(input.getOperatorName()), DimensionOperationLog.Fields.operatorName, input.getOperatorName());
                    if (input.getStartTime() != null) {
                        w.ge(true, "createTime", input.getStartTime());
                    }
                    if (input.getEndTime() != null) {
                        w.le(true, "createTime", input.getEndTime());
                    }
                }),
                input.getPageable()
        );

        List<DimensionOperationLogDto> items = page.getContent().stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return new PagedResultDto<>(page.getTotalElements(), items);
    }

    @Override
    public void log(DimensionOperationType operationType, String targetType, Long targetId, String targetName, String detail) {
        DimensionOperationLog log = new DimensionOperationLog();
        log.setOperationType(operationType.getValue());
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setTargetName(targetName);
        log.setOperationDetail(detail);

        UserContext user = securityUtils.getCurrentUser();
        if (user != null) {
            log.setOperatorId(user.getId());
            log.setOperatorName(user.getName());
            log.setTenantId(user.getTenantId());
        }

        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            log.setClientIp(JakartaServletUtil.getClientIP(request));
        }

        repository.save(log);
    }

    private DimensionOperationLogDto toDto(DimensionOperationLog entity) {
        DimensionOperationLogDto dto = new DimensionOperationLogDto();
        BeanUtil.copyProperties(entity, dto);

        // 设置操作类型名称
        for (DimensionOperationType type : DimensionOperationType.values()) {
            if (type.getValue() == entity.getOperationType()) {
                dto.setOperationTypeName(type.getDisplayName());
                break;
            }
        }

        return dto;
    }
}
