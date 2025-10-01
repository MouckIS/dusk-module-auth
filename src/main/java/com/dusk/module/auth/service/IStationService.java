package com.dusk.module.auth.service;

import com.dusk.commom.rpc.auth.dto.station.StationDto;
import com.dusk.module.auth.dto.station.*;
import com.dusk.common.core.service.ITreeService;
import com.dusk.module.auth.dto.station.*;
import com.dusk.module.auth.entity.Station;
import com.dusk.module.auth.repository.IStationRepository;
import org.springframework.data.domain.Page;

import java.util.List;

public interface IStationService extends ITreeService<Station,IStationRepository> {

    /**
     * 创建厂站
     *
     * @param input
     * @return
     */
    Station createOrUpdate(CreateOrUpdateStationInput input);

    /**
     * 从厂站中删除用户
     *
     * @param input
     */
    void removeUserFromStation(RemoveUserFromStationInput input);

    /**
     * 添加用户到组织机构
     *
     * @param input
     */
    void addUsersToStation(AddUsersToStationInput input);

    /**
     * 获取厂站下的用户
     *
     * @param input
     * @return
     */
    Page<StationUserListDto> getStationUsers(GetStationUsersInput input);

    /**
     * 给前端右上角用的厂站列表，包含默认厂站
     *
     * @param id
     * @return
     */
    List<StationsOfLoginUserDto> getStationsForFrontByUserId(Long id);

    /**
     * 获取所有厂站
     * @return
     */
    List<StationDto> getAllStations();

    Page<StationUserDto> getNotAssignedStationUsers(GetNotAssignedStationUsersInput input);

    StationDto findOneByDisplayName(String displayName);
}