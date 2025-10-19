package com.dusk.module.auth.controller;

import com.dusk.common.core.annotation.Authorize;
import com.dusk.common.core.controller.CruxBaseController;
import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.common.rpc.auth.dto.station.StationDto;
import com.dusk.module.auth.authorization.StationAuthProvider;
import com.dusk.module.auth.dto.station.*;
import com.dusk.module.auth.entity.Station;
import com.dusk.module.auth.mapper.StationMapper;
import com.dusk.module.auth.service.IStationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("station")
@Api(tags="Station",description="厂站")
public class StationController extends CruxBaseController {
	@Resource
	private IStationService stationService;

	private final StationMapper mapper = StationMapper.INSTANCE;

	@GetMapping("getAllStations")
	@ApiOperation("获取所有厂站")
	public List<StationDto> getAllStations() {
		return stationService.getAllStations();
	}

	@GetMapping("getStationUsers")
	@ApiOperation("获取厂站下的用户")
	public PagedResultDto<StationUserListDto> getOrganizationUnitUsers(@Valid GetStationUsersInput input) {
		Page<StationUserListDto> page = stationService.getStationUsers(input);
		return new PagedResultDto<>(page.getTotalElements(), page.getContent());
	}

	@PostMapping("createOrUpdateStation")
	@ApiOperation("新增厂站")
	@Authorize(StationAuthProvider.PAGES_ADMINISTRATION_STATION_MANAGE_STATION)
	public StationDto createOrganizationUnit(@Valid @RequestBody CreateOrUpdateStationInput input) {
		Station station = stationService.createOrUpdate(input);
		return mapper.toDto(station);
	}

	@PostMapping("deleteStation/{id}")
	@ApiOperation("删除厂站")
	@Authorize(StationAuthProvider.PAGES_ADMINISTRATION_STATION_MANAGE_STATION)
	public void deleteOrganizationUnit(@Valid @PathVariable Long id) {
		stationService.deleteById(id);
	}

	@PostMapping("removeUserFromStation")
	@ApiOperation("从厂站删除用户")
	@Authorize(StationAuthProvider.PAGES_ADMINISTRATION_STATION_MANAGE_MEMBERS)
	public void removeUserFromStation(@Valid @RequestBody RemoveUserFromStationInput input) {
		stationService.removeUserFromStation(input);
	}

	@PostMapping("addUsersToStation")
	@ApiOperation("添加用户到厂站")
	@Authorize(StationAuthProvider.PAGES_ADMINISTRATION_STATION_MANAGE_MEMBERS)
	public void addUsersToStation(@Valid @RequestBody AddUsersToStationInput input) {
		stationService.addUsersToStation(input);
	}

	@ApiOperation("获取当前用户下的所有厂站")
	@GetMapping("getStationsOfLoginUser")
	public List<StationsOfLoginUserDto> getStationsOfLoginUser() {
		return stationService.getStationsForFrontByUserId(getCurrentUser().getId());
	}

	@GetMapping("getNotAssignedStationUsers")
	@ApiOperation("获取厂站未分配的用户")
	public PagedResultDto<StationUserDto> getNotAssignedStationUsers(@Valid GetNotAssignedStationUsersInput input) {
		Page<StationUserDto> page = stationService.getNotAssignedStationUsers(input);
		return new PagedResultDto<>(page.getTotalElements(), page.getContent());
	}
}