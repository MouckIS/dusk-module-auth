package com.dusk.module.auth.service;

import com.dusk.module.auth.dto.orga.OrganizationUnitDto;

import java.util.List;

/**
 * @author duanxiaokang
 * @date 2021/5/21
 */

public interface ICustomerRpcService {
    void saveCustomer(OrganizationUnitDto input);

    void deleteCustomer(Long id);

    List<OrganizationUnitDto> getCustomerList(String code);

    List<OrganizationUnitDto> getCurrentCustomerList(Long orgId);

    OrganizationUnitDto getOne(Long id);
}
