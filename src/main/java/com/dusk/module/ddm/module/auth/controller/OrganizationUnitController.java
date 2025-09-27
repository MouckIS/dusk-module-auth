package com.dusk.module.ddm.module.auth.controller;

import com.dusk.module.auth.dto.orga.*;
import com.github.dozermapper.core.Mapper;
import io.swagger.annotations.*;
import com.dusk.common.core.annotation.Authorize;
import com.dusk.common.core.controller.CruxBaseController;
import com.dusk.common.core.dto.EntityDto;
import com.dusk.common.core.dto.ListResultDto;
import com.dusk.common.core.dto.NameValueDto;
import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.common.core.entity.TreeEntity;
import com.dusk.common.core.jpa.Specifications;
import com.dusk.common.core.utils.DozerUtils;
import com.dusk.common.module.auth.dto.orga.GetOrganizationUnitUsersInput;
import com.dusk.common.module.auth.dto.orga.OrganizationUnitDto;
import com.dusk.common.module.auth.dto.orga.OrganizationUnitUserListDto;
import com.dusk.common.module.auth.dto.station.StationDto;
import com.dusk.common.module.auth.enums.EUnitType;
import com.dusk.module.auth.authorization.OrganizationUnitAuthProvider;
import com.dusk.module.auth.dto.orga.*;
import com.dusk.module.auth.dto.station.StationsOfLoginUserDto;
import com.dusk.module.auth.entity.OrganizationManager;
import com.dusk.module.auth.entity.OrganizationUnit;
import com.dusk.module.auth.entity.User;
import com.dusk.module.auth.repository.IOrganizationManagerRepository;
import com.dusk.module.auth.service.IOrganizationUnitService;
import com.dusk.module.auth.service.IStationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author kefuming
 * @date 2020-05-13 13:44
 */
@RestController
@RequestMapping("organizationUnit")
@Api(tags = "OrganizationUnit", description = "组织机构")
public class OrganizationUnitController extends CruxBaseController {
    @Autowired
    private IOrganizationUnitService organizationUnitService;
    @Autowired
    private IOrganizationManagerRepository organizationManagerRepository;
    @Autowired
    private Mapper dozerMapper;
    @Autowired
    private IStationService stationService;

    @GetMapping("getOrganizationUnits")
    @ApiOperation("获取本单位所有组织机构")
    public ListResultDto<OrganizationStationUnitDto> getOrganizationUnits() {
        List<OrganizationUnit> organizationUnitList = organizationUnitService.findAll(
                Specifications.where(e->e.eq(OrganizationUnit.Fields.type, EUnitType.Inner)),
                Sort.by(TreeEntity.Fields.sortIndex, TreeEntity.Fields.displayName));
        Map<Long, Long> map = organizationManagerRepository.findAll().stream()
                .collect(Collectors.toMap(OrganizationManager::getOrgId, OrganizationManager::getUserId));

        return DozerUtils.mapToListResultDto(dozerMapper, organizationUnitList, OrganizationStationUnitDto.class, (unit, dto) -> {
            // 设置管理的组织id
            Long orgId = unit.getId();
            if (map.containsKey(orgId)) {
                dto.setManagerId(map.get(orgId));
            }
        });
    }


    @PutMapping("getOrganizationUnitUsers")
    @ApiOperation("获取组织机构下的用户")
    public PagedResultDto<OrganizationUnitUserListDto> getOrganizationUnitUsers(@Valid @RequestBody GetOrganizationUnitUsersInput input) {
        Page<OrganizationUnitUserListDto> page = organizationUnitService.getOrganizationUnitUsers(input);
        return DozerUtils.mapToPagedResultDto(dozerMapper, page, OrganizationUnitUserListDto.class);
    }

    @PutMapping("getOrganizationUnitUsersInfo")
    @ApiOperation("获取组织机构下的用户(部分信息)")
    public PagedResultDto<OrganizationUnitUserInfoListDto> getOrganizationUnitUsersInfo(@Valid @RequestBody GetOrganizationUnitUsersExtInput input) {
        Page<OrganizationUnitUserInfoListDto> page = organizationUnitService.getOrganizationUnitUsersInfo(input);
        return DozerUtils.mapToPagedResultDto(dozerMapper, page, OrganizationUnitUserInfoListDto.class);
    }

    @PutMapping("getOrganizationUnitUsersForSelect")
    @ApiOperation("组织机构的用户选择下拉列表")
    public PagedResultDto<OrganizationUnitUserForSelectDto> getOrganizationUnitUsersForSelect(@Valid @RequestBody GetOrganizationUnitUsersForSelectInput input) {
        Page<OrganizationUnitUserForSelectDto> page = organizationUnitService.getOrganizationUnitUsersForSelect(input);
        return DozerUtils.mapToPagedResultDto(dozerMapper, page, OrganizationUnitUserForSelectDto.class);
    }

    @PostMapping("createOrganizationUnit")
    @ApiOperation("新增组织机构")
    @authorize(OrganizationUnitAuthProvider.PAGES_ADMINISTRATION_ORGANIZATIONUNITS_MANAGEORGANIZATIONTREE)
    public OrganizationUnitDto createOrganizationUnit(@Valid @RequestBody CreateOrganizationUnitInput input) {
        OrganizationUnit organizationUnit = organizationUnitService.create(input);
        return dozerMapper.map(organizationUnit, OrganizationUnitDto.class);
    }


    @PutMapping("updateOrganizationUnit")
    @ApiOperation("更新组织机构")
    @authorize(OrganizationUnitAuthProvider.PAGES_ADMINISTRATION_ORGANIZATIONUNITS_MANAGEORGANIZATIONTREE)
    public OrganizationUnitDto updateOrganizationUnit(@Valid @RequestBody UpdateOrganizationUnitInput input) {
        OrganizationUnit organizationUnit = organizationUnitService.update(input);
        return dozerMapper.map(organizationUnit, OrganizationUnitDto.class);
    }

    @PutMapping("moveOrganizationUnit")
    @ApiOperation("移动组织机构")
    @authorize(OrganizationUnitAuthProvider.PAGES_ADMINISTRATION_ORGANIZATIONUNITS_MANAGEORGANIZATIONTREE)
    public OrganizationUnitDto moveOrganizationUnit(@Valid @RequestBody MoveOrganizationUnitInput input) {
        OrganizationUnit organizationUnit = organizationUnitService.move(input);
        return dozerMapper.map(organizationUnit, OrganizationUnitDto.class);
    }

    @DeleteMapping("deleteOrganizationUnit")
    @ApiOperation("删除组织机构")
    @authorize(OrganizationUnitAuthProvider.PAGES_ADMINISTRATION_ORGANIZATIONUNITS_MANAGEORGANIZATIONTREE)
    public void deleteOrganizationUnit(@Valid @RequestBody EntityDto input) {
        organizationUnitService.deleteOrgById(input.getId());
    }

    @DeleteMapping("removeUserFromOrganizationUnit")
    @ApiOperation("从组织机构中删除用户")
    @authorize(OrganizationUnitAuthProvider.PAGES_ADMINISTRATION_ORGANIZATIONUNITS_MANAGEMEMBERS)
    public void removeUserFromOrganizationUnit(@Valid @RequestBody UserToOrganizationUnitInput input) {
        organizationUnitService.removeUserFromOrganizationUnit(input);
    }

    @PutMapping("addUsersToOrganizationUnit")
    @ApiOperation("添加用户到组织机构")
    @authorize(OrganizationUnitAuthProvider.PAGES_ADMINISTRATION_ORGANIZATIONUNITS_MANAGEMEMBERS)
    public void addUsersToOrganizationUnit(@Valid @RequestBody UsersToOrganizationUnitInput input) {
        organizationUnitService.addUsersToOrganizationUnit(input);
    }

    @ApiOperation("获取组织机构下的用户, 返回键值对数据")
    @PutMapping("findUsers")
    public PagedResultDto<NameValueDto<Long>> findUsers(@Valid @RequestBody GetOrganizationUnitUsersInput input) {
        Page<User> page = organizationUnitService.findUsers(input);
        List<NameValueDto<Long>> list = new ArrayList<>();
        page.getContent().forEach(user -> {
            NameValueDto<Long> nameValueDto = new NameValueDto<>();
            nameValueDto.setName(user.getName());
            nameValueDto.setValue(user.getId());
            list.add(nameValueDto);
        });
        return new PagedResultDto<>(page.getTotalElements(), list);
    }

    @ApiOperation("根据用户ID获取该用户所属的所有组织机构")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "用户id")
    })
    @GetMapping("getOrganizationUnitsByUser")
    public List<OrganizationUnitDto> getOrganizationUnitsByUser(EntityDto input) {
        return organizationUnitService.getOrganizationUnitsByUserId(input.getId());
    }

    @ApiOperation("获取组织机构节点的父级[包括自身]")
    @GetMapping("getParentOrganizations")
    public List<ParentOrganizationOutput> getParentOrganizations(EntityDto input) {
        return DozerUtils.mapList(dozerMapper, organizationUnitService.getParentOrganizations(input), ParentOrganizationOutput.class);
    }

    @ApiOperation("设置厂站可用/不可用")
    @PostMapping("setStationEnabled/{id}")
    @authorize(OrganizationUnitAuthProvider.PAGES_ADMINISTRATION_ORGANIZATIONUNITS_STATION_ENABLED)
    public void setStationEnabled(@Valid @PathVariable Long id, boolean enabled) {
        organizationUnitService.setStationEnabled(id, enabled);
    }

    @ApiOperation("获取厂站下拉列表")
    @GetMapping("getStations")
    public List<NameValueDto<Long>> getStations() {
        List<StationDto> stations = stationService.getAllStations();
        return changeToNameValueList(stations);
    }

    @ApiOperation("获取当前用户下的所有厂站")
    @GetMapping("getStationsOfLoginUser")
    public List<StationsOfLoginUserDto> getStationsOfLoginUser() {
        return stationService.getStationsForFrontByUserId(getCurrentUser().getId());
    }


    private List<NameValueDto<Long>> changeToNameValueList(List<StationDto> list) {
        var result = new ArrayList<NameValueDto<Long>>();
        list.forEach(e -> {
            NameValueDto<Long> nameValueDto = new NameValueDto<>();
            nameValueDto.setName(e.getDisplayName());
            nameValueDto.setValue(e.getId());
            result.add(nameValueDto);
        });
        return result;
    }

    @ApiOperation("获取当前用户下的所有厂站结构树-（包含普通节点的最小树）")
    @GetMapping("getStationTreeOfLoginUser")
    public List<OrganizationUnitDto> getStationTreeOfLoginUser() {
        List<OrganizationUnit> organizationUnits = organizationUnitService.getStationTreeByUserId(getCurrentUser().getId());
        return DozerUtils.mapList(dozerMapper, organizationUnits, OrganizationUnitDto.class);
    }
}
