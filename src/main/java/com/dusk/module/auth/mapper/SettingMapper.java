package com.dusk.module.auth.mapper;

import com.dusk.module.auth.dto.setting.SettingDto;
import com.dusk.module.auth.entity.Setting;
import com.dusk.module.auth.setting.SettingInfo;
import com.dusk.module.ddm.dto.SettingDefinition;
import org.mapstruct.Mapper;

@Mapper
public interface SettingMapper {
    SettingMapper INSTANCE = org.mapstruct.factory.Mappers.getMapper(SettingMapper.class);

    SettingDto toDto(Setting entity);

    Setting toEntity(SettingDto dto);

    SettingDto definitionToDto(SettingDefinition entity);

    SettingInfo toInfo(Setting entity);

    Setting infoDtoToEntity(SettingInfo info);
}
