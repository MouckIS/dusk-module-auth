package com.dusk.module.auth.service;


import com.dusk.module.auth.dto.station.StationDto;

import java.util.List;

/**
 * 厂站rpc接口
 *
 * @author kefuming
 * @date 2022/10/11 18:59
 */
public interface IStationRpcService {
    List<StationDto> getAllStations();

    StationDto findOneByDisplayName(String displayName);

    StationDto findOneById(Long id);

    List<StationDto> findByIds(List<Long> ids);

    List<StationDto> getStationsByUserId(Long userId);

    StationDto getCurrentStation();

    List<StationDto> getStationsByParentId(Long parentId);
}
