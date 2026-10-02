package com.dusk.module.auth.mapper;

import com.dusk.module.auth.dto.datadisplay.DataDisplayItemDto;
import com.dusk.module.auth.dto.datadisplay.UpdateDataDisplaySetDto;
import com.dusk.module.auth.entity.datadisplay.DataDisplaySet;
import org.mapstruct.Mapper;

/**
 * @author : kefuming
 * @date : 2025/10/19 21:44
 */
@Mapper
public interface DataDisplaySetMapper {
    DataDisplaySetMapper INSTANCE = org.mapstruct.factory.Mappers.getMapper(DataDisplaySetMapper.class);

    DataDisplaySet toEntity(UpdateDataDisplaySetDto dto);

    DataDisplayItemDto toItemDto(DataDisplaySet entity);
}
