package com.dusk.module.auth.mapper;

import com.dusk.module.auth.dto.sysno.SerialNoDto;
import com.dusk.module.auth.entity.SerialNo;
import org.mapstruct.Mapper;

/**
 * @author : kefuming
 * @date : 2025/10/19 21:04
 */
@Mapper
public interface SerialNoMapper {
    SerialNoMapper INSTANCE = org.mapstruct.factory.Mappers.getMapper(SerialNoMapper.class);

    SerialNoDto toDto(SerialNo entity);
}
