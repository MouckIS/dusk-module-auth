package com.dusk.module.auth.service.impl;

import com.dusk.common.core.jpa.Specifications;
import com.dusk.common.core.utils.MapperUtil;
import com.dusk.common.rpc.auth.dto.orga.OrganizationUnitDto;
import com.dusk.common.rpc.auth.service.ICustomerRpcService;
import com.dusk.module.auth.dto.orga.CreateOrganizationUnitInput;
import com.dusk.module.auth.dto.orga.UpdateOrganizationUnitInput;
import com.dusk.module.auth.entity.OrganizationUnit;
import com.dusk.module.auth.mapper.OrganizationMapper;
import com.dusk.module.auth.service.IOrganizationUnitService;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author kefuming
 * @date 2021/5/21
 */
@DubboService
@Transactional
public class CustomerRpcServiceImpl implements ICustomerRpcService {
    @Resource
    private IOrganizationUnitService organizationUnitService;

    private final OrganizationMapper mapper = OrganizationMapper.INSTANCE;

    @Override
    public void saveCustomer(OrganizationUnitDto input) {
        OrganizationUnit org;
        if(input.getId() != null){
            UpdateOrganizationUnitInput updateOrganizationUnitInput = mapper.toUpdateInput(input);
            org = organizationUnitService.update(updateOrganizationUnitInput);
        }else{
            CreateOrganizationUnitInput createOrganizationUnitInput = mapper.toCreateInput(input);
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
        return MapperUtil.mapList(organizationUnitList, mapper::toDto);
    }

    @Override
    public List<OrganizationUnitDto> getCurrentCustomerList(Long orgId) {
        OrganizationUnit org = organizationUnitService.getOne(orgId);
        OrganizationUnitDto organizationUnitDto = mapper.toDto(org);
        List<OrganizationUnitDto> result = organizationUnitService.getStationsByParentId(orgId);
        result.add(organizationUnitDto);
        return result;
    }

    @Override
    public OrganizationUnitDto getOne(Long id) {
        OrganizationUnit organizationUnit = organizationUnitService.getOne(id);
        return mapper.toDto(organizationUnit);
    }
}
