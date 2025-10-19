package com.dusk.module.auth.mapper;

import com.dusk.common.rpc.auth.dto.station.StationDto;
import com.dusk.module.auth.dto.station.StationsOfLoginUserDto;
import com.dusk.module.auth.entity.Station;
import org.mapstruct.Mapper;

@Mapper
public interface StationMapper {
    StationMapper INSTANCE = org.mapstruct.factory.Mappers.getMapper(StationMapper.class);

    StationDto toDto(Station entity);

    StationsOfLoginUserDto toStationsOfLoginUserDto(Station entity);
}
