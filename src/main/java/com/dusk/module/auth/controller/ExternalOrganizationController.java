package com.dusk.module.auth.controller;

import com.dusk.module.auth.dto.orga.*;
import com.github.dozermapper.core.Mapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import com.dusk.common.framework.annotation.Authorize;
import com.dusk.common.framework.controller.CruxBaseController;
import com.dusk.common.framework.dto.EntityDto;
import com.dusk.common.framework.dto.ListResultDto;
import com.dusk.common.module.auth.dto.orga.OrganizationUnitDto;
import com.dusk.module.auth.authorization.ExternalManagerAuthProvider;
import com.dusk.module.auth.dto.orga.*;
import com.dusk.module.auth.entity.OrganizationUnit;
import com.dusk.module.auth.service.IOrganizationUnitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * @Author kefuming
 * @CreateTime 2022-10-28
 */
@RestController
@Api(tags = "ExternalOrganization", description = "外部组织机构")
@RequestMapping("externalOrganization")
@Authorize(ExternalManagerAuthProvider.PAGES_EXTERNAL_ORGANIZATION)
public class ExternalOrganizationController extends CruxBaseController {
    @Autowired
    private IOrganizationUnitService organizationUnitService;
    @Autowired
    private Mapper dozerMapper;

    @GetMapping("getExternalOrganizationUnits")
    @ApiOperation("获取外单位所有组织机构")
    public ListResultDto<OrganizationStationUnitDto> getExternalOrganizationUnits() {
        return organizationUnitService.getExternalOrganizationUnits();
    }

    @PostMapping("createExternalOrganizationUnit")
    @ApiOperation("新增外单位组织机构")
    @Authorize(ExternalManagerAuthProvider.PAGES_EXTERNAL_ORGANIZATION_MANAGE_ORGANIZATION_TREE)
    public OrganizationUnitDto createExternalOrganizationUnit(@Valid @RequestBody CreateOrganizationUnitInput input) {
        OrganizationUnit organizationUnit = organizationUnitService.createExternalOrganization(input);
        return dozerMapper.map(organizationUnit, OrganizationUnitDto.class);
    }

    @PutMapping("updateOrganizationUnit")
    @ApiOperation("更新组织机构")
    @Authorize(ExternalManagerAuthProvider.PAGES_EXTERNAL_ORGANIZATION_MANAGE_ORGANIZATION_TREE)
    public OrganizationUnitDto updateOrganizationUnit(@Valid @RequestBody UpdateOrganizationUnitInput input) {
        OrganizationUnit organizationUnit = organizationUnitService.update(input);
        return dozerMapper.map(organizationUnit, OrganizationUnitDto.class);
    }

    @PutMapping("moveOrganizationUnit")
    @ApiOperation("移动组织机构")
    @Authorize(ExternalManagerAuthProvider.PAGES_EXTERNAL_ORGANIZATION_MANAGE_ORGANIZATION_TREE)
    public OrganizationUnitDto moveOrganizationUnit(@Valid @RequestBody MoveOrganizationUnitInput input) {
        OrganizationUnit organizationUnit = organizationUnitService.move(input);
        return dozerMapper.map(organizationUnit, OrganizationUnitDto.class);
    }

    @DeleteMapping("deleteOrganizationUnit")
    @ApiOperation("删除组织机构")
    @Authorize(ExternalManagerAuthProvider.PAGES_EXTERNAL_ORGANIZATION_MANAGE_ORGANIZATION_TREE)
    public void deleteOrganizationUnit(@Valid @RequestBody EntityDto input) {
        organizationUnitService.deleteOrgById(input.getId());
    }

    @DeleteMapping("removeUserFromOrganizationUnit")
    @ApiOperation("从组织机构中删除用户")
    @Authorize(ExternalManagerAuthProvider.PAGES_EXTERNAL_ORGANIZATION_MANAGE_ORGANIZATION_TREE)
    public void removeUserFromOrganizationUnit(@Valid @RequestBody UserToOrganizationUnitInput input) {
        organizationUnitService.removeUserFromOrganizationUnit(input);
    }

    @PutMapping("addUsersToOrganizationUnit")
    @ApiOperation("添加用户到组织机构")
    @Authorize(ExternalManagerAuthProvider.PAGES_EXTERNAL_ORGANIZATION_MANAGE_ORGANIZATION_TREE)
    public void addUsersToOrganizationUnit(@Valid @RequestBody UsersToOrganizationUnitInput input) {
        organizationUnitService.addUsersToOrganizationUnit(input);
    }
}
