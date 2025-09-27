package com.dusk.module.ddm.module.auth.service.impl;

import com.github.dozermapper.core.Mapper;
import org.apache.dubbo.config.annotation.Service;
import com.dusk.common.core.jpa.Specifications;
import com.dusk.common.core.utils.DozerUtils;
import com.dusk.common.module.auth.dto.orga.OrganizationUnitDto;
import com.dusk.common.module.auth.service.ICustomerRpcService;
import com.dusk.module.auth.dto.orga.CreateOrganizationUnitInput;
import com.dusk.module.auth.dto.orga.UpdateOrganizationUnitInput;
import com.dusk.module.auth.entity.OrganizationUnit;
import com.dusk.module.auth.service.IOrganizationUnitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author kefuming
 * @date 2021/5/21
 */
@Service
@Transactional
public class CustomerRpcServiceImpl implements ICustomerRpcService {

    @Autowired
    IOrganizationUnitService organizationUnitService;
    @Autowired
    Mapper mapper;

    @Override
    public void saveCustomer(OrganizationUnitDto input) {
        OrganizationUnit org;
        if(input.getId() != null){
            UpdateOrganizationUnitInput updateOrganizationUnitInput = mapper.map(input, UpdateOrganizationUnitInput.class);
            org = organizationUnitService.update(updateOrganizationUnitInput);
        }else{
            CreateOrganizationUnitInput createOrganizationUnitInput = mapper.map(input, CreateOrganizationUnitInput.class);
            org = organizationUnitService.create(createOrganizationUnitInput);
        }
        org.setCode(input.getCode());
        organizationUnitService.save(org);
    }

    @Override
    public void deleteCustomer(Long id) {
        organizationUnitService.deleteById(id);
    }

    @Override
    public List<OrganizationUnitDto> getCustomerList(String code) {
        List<OrganizationUnit> organizationUnitList = organizationUnitService.findAll(Specifications.where(e -> {
            e.startingWith(OrganizationUnit.Fields.code, code);
        }));
        return DozerUtils.mapList(mapper, organizationUnitList, OrganizationUnitDto.class);
    }

    @Override
    public List<OrganizationUnitDto> getCurrentCustomerList(Long orgId) {
        OrganizationUnit org = organizationUnitService.getOne(orgId);
        OrganizationUnitDto organizationUnitDto = mapper.map(org, OrganizationUnitDto.class);
        List<OrganizationUnitDto> result = organizationUnitService.getStationsByParentId(orgId);
        result.add(organizationUnitDto);
        return result;
    }

    @Override
    public OrganizationUnitDto getOne(Long id) {
        OrganizationUnit organizationUnit = organizationUnitService.getOne(id);
        return mapper.map(organizationUnit, OrganizationUnitDto.class);
    }
}
