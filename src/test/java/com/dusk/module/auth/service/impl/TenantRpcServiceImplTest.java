package com.dusk.module.auth.service.impl;

import com.dusk.common.rpc.auth.dto.TenantInfoDto;
import com.dusk.module.auth.entity.Tenant;
import com.dusk.module.auth.repository.ITenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * {@link TenantRpcServiceImpl} 单元测试，目标：100% 分支覆盖。
 */
@ExtendWith(MockitoExtension.class)
class TenantRpcServiceImplTest {

    @Mock
    private ITenantRepository tenantRepository;

    private TenantRpcServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TenantRpcServiceImpl();
        ReflectionTestUtils.setField(service, "tenantRepository", tenantRepository);
    }

    private static Tenant tenant(long id, String name) {
        Tenant tenant = new Tenant();
        tenant.setId(id);
        tenant.setTenantName(name);
        tenant.setName(name);
        return tenant;
    }

    @Test
    @DisplayName("findById：存在租户时返回 DTO")
    void findByIdReturnsDtoWhenPresent() {
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant(1L, "t1")));

        assertThat(service.findById(1L)).isNotNull();
    }

    @Test
    @DisplayName("findById：租户不存在时返回 null")
    void findByIdReturnsNullWhenAbsent() {
        when(tenantRepository.findById(1L)).thenReturn(Optional.empty());

        assertThat(service.findById(1L)).isNull();
    }

    @Test
    @DisplayName("findByTenantName：存在租户时返回 DTO")
    void findByTenantNameReturnsDtoWhenPresent() {
        when(tenantRepository.findByTenantName("t1")).thenReturn(Optional.of(tenant(1L, "t1")));

        assertThat(service.findByTenantName("t1")).isNotNull();
    }

    @Test
    @DisplayName("findByTenantName：租户不存在时返回 null")
    void findByTenantNameReturnsNullWhenAbsent() {
        when(tenantRepository.findByTenantName("t1")).thenReturn(Optional.empty());

        assertThat(service.findByTenantName("t1")).isNull();
    }

    @Test
    @DisplayName("findAll：无租户时返回空列表")
    void findAllReturnsEmptyListWhenNoTenant() {
        when(tenantRepository.findAll()).thenReturn(Collections.emptyList());

        List<TenantInfoDto> result = service.findAll();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAll：存在租户时逐个映射")
    void findAllMapsTenants() {
        when(tenantRepository.findAll()).thenReturn(List.of(tenant(1L, "t1"), tenant(2L, "t2")));

        List<TenantInfoDto> result = service.findAll();

        assertThat(result).hasSize(2);
    }
}
