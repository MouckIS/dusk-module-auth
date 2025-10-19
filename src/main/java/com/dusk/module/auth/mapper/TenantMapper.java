package com.dusk.module.auth.mapper;

import com.dusk.common.rpc.auth.dto.TenantInfoDto;
import com.dusk.module.auth.dto.configuration.TenantConfigDto;
import com.dusk.module.auth.dto.tenant.TenantEditDto;
import com.dusk.module.auth.dto.tenant.TenantListDto;
import com.dusk.module.auth.entity.Tenant;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author : kefuming
 * @date : 2025/10/8 11:20
 */
@Mapper
public interface TenantMapper {
    TenantMapper INSTANCE = Mappers.getMapper(TenantMapper.class);

    TenantConfigDto entityToConfigDto(Tenant entity);

    TenantListDto toListDto(Tenant entity);

    TenantEditDto toEditDto(Tenant entity);

    TenantInfoDto toInfoDto(Tenant entity);
}
