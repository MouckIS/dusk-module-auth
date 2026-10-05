package com.dusk.module.auth.mapper;

import com.dusk.module.auth.dto.auditlog.AuditLogDetailDto;
import com.dusk.module.auth.dto.auditlog.AuditLogExportDto;
import org.mapstruct.Mapper;

/**
 * @author : kefuming
 * @date : 2025/10/19 21:42
 */
@Mapper
public interface AuditLogMapper {
    AuditLogMapper INSTANCE = org.mapstruct.factory.Mappers.getMapper(AuditLogMapper.class);

    AuditLogExportDto detailDtoToExportDto(AuditLogDetailDto dto);
}
