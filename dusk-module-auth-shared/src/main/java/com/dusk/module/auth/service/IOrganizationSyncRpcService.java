package com.dusk.module.auth.service;


import com.dusk.module.auth.dto.orga.OrganizationUnitDto;

public interface IOrganizationSyncRpcService {
    OrganizationUnitDto create(OrganizationUnitDto input);

    OrganizationUnitDto update(OrganizationUnitDto input);

    OrganizationUnitDto move(OrganizationUnitDto input);

    void deleteById(Long id);
}
