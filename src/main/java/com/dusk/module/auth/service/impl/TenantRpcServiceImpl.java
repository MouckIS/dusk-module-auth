package com.dusk.module.auth.service.impl;

import com.dusk.commom.rpc.auth.dto.TenantInfoDto;
import com.dusk.commom.rpc.auth.service.ITenantRpcService;
import com.github.dozermapper.core.Mapper;
import org.apache.dubbo.config.annotation.Service;
import com.dusk.common.core.utils.DozerUtils;
import com.dusk.module.auth.entity.Tenant;
import com.dusk.module.auth.repository.ITenantRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author kefuming
 * @date 2020-07-24 16:35
 */
@Service
public class TenantRpcServiceImpl implements ITenantRpcService {
    @Autowired
    ITenantRepository  tenantRepository;
    @Autowired
    Mapper dozerMapper;
    @Override
    public TenantInfoDto findById(Long id) {
        Optional<Tenant> tenant = tenantRepository.findById(id);
        return tenant.map(value ->{
            TenantInfoDto dto=  dozerMapper.map(value, TenantInfoDto.class);
            dto.setEnabled(value.enabled());
            return dto;
        } ).orElse(null);
    }

    @Override
    public TenantInfoDto findByTenantName(String name) {
        Optional<Tenant> tenant = tenantRepository.findByTenantName(name);
        return tenant.map(value -> {
            TenantInfoDto dto = dozerMapper.map(value, TenantInfoDto.class);
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
        return DozerUtils.mapList(dozerMapper, allTenant, TenantInfoDto.class, (s, t) -> t.setEnabled(s.enabled()));
    }
}
