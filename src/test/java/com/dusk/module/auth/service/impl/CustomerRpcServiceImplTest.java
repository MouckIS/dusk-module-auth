package com.dusk.module.auth.service.impl;

import com.dusk.common.rpc.auth.dto.orga.OrganizationUnitDto;
import com.dusk.module.auth.entity.OrganizationUnit;
import com.dusk.module.auth.service.IOrganizationUnitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link CustomerRpcServiceImpl} 单元测试，目标：100% 分支覆盖。
 */
@ExtendWith(MockitoExtension.class)
class CustomerRpcServiceImplTest {

    @Mock
    private IOrganizationUnitService organizationUnitService;

    private CustomerRpcServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CustomerRpcServiceImpl();
        ReflectionTestUtils.setField(service, "organizationUnitService", organizationUnitService);
    }

    private static OrganizationUnitDto dto(Long id) {
        OrganizationUnitDto dto = new OrganizationUnitDto();
        dto.setId(id);
        dto.setCode("C1");
        return dto;
    }

    @Test
    @DisplayName("saveCustomer：id 为空时走新增分支")
    void saveCustomerCreatesWhenIdIsNull() {
        OrganizationUnit created = new OrganizationUnit();
        when(organizationUnitService.create(any())).thenReturn(created);

        service.saveCustomer(dto(null));

        verify(organizationUnitService).create(any());
        verify(organizationUnitService).save(created);
    }

    @Test
    @DisplayName("saveCustomer：id 非空时走更新分支")
    void saveCustomerUpdatesWhenIdIsNotNull() {
        OrganizationUnit updated = new OrganizationUnit();
        when(organizationUnitService.update(any())).thenReturn(updated);

        service.saveCustomer(dto(1L));

        verify(organizationUnitService).update(any());
        verify(organizationUnitService).save(updated);
    }

    @Test
    @DisplayName("deleteCustomer：透传删除")
    void deleteCustomerDelegates() {
        service.deleteCustomer(1L);

        verify(organizationUnitService).deleteById(1L);
    }

    @Test
    @DisplayName("getCustomerList：按 code 前缀查询并映射")
    void getCustomerListMapsResults() {
        OrganizationUnit unit = new OrganizationUnit();
        unit.setId(1L);
        when(organizationUnitService.findAll(any(Specification.class))).thenReturn(List.of(unit));

        assertThat(service.getCustomerList("C")).hasSize(1);
    }

    @Test
    @DisplayName("getCurrentCustomerList：返回下级厂站并追加当前节点")
    void getCurrentCustomerListAppendsSelf() {
        OrganizationUnit unit = new OrganizationUnit();
        unit.setId(1L);
        when(organizationUnitService.getOne(1L)).thenReturn(unit);
        when(organizationUnitService.getStationsByParentId(1L)).thenReturn(new ArrayList<>());

        List<OrganizationUnitDto> result = service.getCurrentCustomerList(1L);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("getOne：返回映射后的 DTO")
    void getOneMapsEntity() {
        OrganizationUnit unit = new OrganizationUnit();
        unit.setId(1L);
        when(organizationUnitService.getOne(1L)).thenReturn(unit);

        assertThat(service.getOne(1L)).isNotNull();
    }
}
