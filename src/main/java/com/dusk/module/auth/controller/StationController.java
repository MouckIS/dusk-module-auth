package com.dusk.module.auth.controller;

import com.dusk.module.auth.dto.station.*;
import com.github.dozermapper.core.Mapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import com.dusk.common.framework.annotation.Authorize;
import com.dusk.common.framework.controller.CruxBaseController;
import com.dusk.common.framework.dto.PagedResultDto;
import com.dusk.common.framework.utils.DozerUtils;
import com.dusk.module.auth.authorization.StationAuthProvider;
import com.dusk.module.auth.dto.station.*;
import com.dusk.common.module.auth.dto.station.StationDto;
import com.dusk.module.auth.entity.Station;
import com.dusk.module.auth.service.IStationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("station")
@Api(tags="Station",description="厂站")
public class StationController extends CruxBaseController {
	@Autowired
	private IStationService stationService ;
	@Autowired
	private Mapper dozerMapper;

	@GetMapping("getAllStations")
	@ApiOperation("获取所有厂站")
	public List<StationDto> getAllStations() {
		return stationService.getAllStations();
	}

	@GetMapping("getStationUsers")
	@ApiOperation("获取厂站下的用户")
	public PagedResultDto<StationUserListDto> getOrganizationUnitUsers(@Valid GetStationUsersInput input) {
		Page<StationUserListDto> page = stationService.getStationUsers(input);
		return DozerUtils.mapToPagedResultDto(dozerMapper, page, StationUserListDto.class);
	}

	@PostMapping("createOrUpdateStation")
	@ApiOperation("新增厂站")
	@Authorize(StationAuthProvider.PAGES_ADMINISTRATION_STATION_MANAGE_STATION)
	public StationDto createOrganizationUnit(@Valid @RequestBody CreateOrUpdateStationInput input) {
		Station station = stationService.createOrUpdate(input);
		return dozerMapper.map(station, StationDto.class);
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
		return DozerUtils.mapToPagedResultDto(dozerMapper, page, StationUserDto.class);
	}
}