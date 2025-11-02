package com.dusk.module.auth.service.impl;

import com.dusk.common.core.utils.MapperUtil;
import com.dusk.common.rpc.auth.dto.TenantInfoDto;
import com.dusk.common.rpc.auth.service.ITenantRpcService;
import com.dusk.module.auth.entity.Tenant;
import com.dusk.module.auth.mapper.TenantMapper;
import com.dusk.module.auth.repository.ITenantRepository;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author kefuming
 * @date 2020-07-24 16:35
 */
@DubboService
public class TenantRpcServiceImpl implements ITenantRpcService {
    @Resource
    private ITenantRepository tenantRepository;

    private final TenantMapper mapper = TenantMapper.INSTANCE;

    @Override
    public TenantInfoDto findById(Long id) {
        Optional<Tenant> tenant = tenantRepository.findById(id);
        return tenant.map(value -> {
            TenantInfoDto dto = mapper.toInfoDto(value);
            dto.setEnabled(value.enabled());
            return dto;
        }).orElse(null);
    }

    @Override
    public TenantInfoDto findByTenantName(String name) {
        Optional<Tenant> tenant = tenantRepository.findByTenantName(name);
        return tenant.map(value -> {
            TenantInfoDto dto = mapper.toInfoDto(value);
            dto.setEnabled(value.enabled());
            return dto;
        }).orElse(null);
    }

    @Override
    public List<TenantInfoDto> findAll() {
        List<Tenant> allTenant = tenantRepository.findAll();
        if (allTenant.isEmpty()) {
            return new ArrayList<>();
        }
        return MapperUtil.mapList(allTenant, mapper::toInfoDto, (s, t) -> t.setEnabled(s.enabled()));
    }
}
