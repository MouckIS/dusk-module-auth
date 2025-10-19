package com.dusk.module.auth.mapper;

import com.dusk.module.auth.dto.dashboard.*;
import com.dusk.module.auth.entity.dashboard.*;
import org.mapstruct.Mapper;

/**
 * @author : kefuming
 * @date : 2025/10/19 20:13
 */
@Mapper
public interface DashboardMapper {
    DashboardMapper INSTANCE = org.mapstruct.factory.Mappers.getMapper(DashboardMapper.class);

    ClassifyDetailDto toDetailDto(DashboardClassify entity);

    DashboardClassify createDtoToEntity(CreateOrUpdateClassify dto);

    ThemeDetailDto toThemeDetailDto(DashboardTheme entity);

    DashboardZone toZoneEntity(CreateOrUpdateZone dto);

    DashboardZoneItemRef toZoneItemRefEntity(CreateOrUpdateZoneItemRef dto);

    ZoneItemDetailDto toZoneItemDetailDto(DashboardZoneItemRef entity);

    ZoneDetailDto toZoneDetailDto(DashboardZone entity);

    ThemeListDto toListDto(DashboardTheme entity);

    ModuleDetailDto toModuleDetailDto(DashboardModule entity);

    ModuleItemListDto toModuleListDto(DashboardModuleItem entity);

    DashboardModuleItem createModuleItemEntity(CreateOrUpdateModuleItem dto);

    DashboardModule detailDtoToEntity(ModuleDetailDto dto);

    CreateOrUpdateModuleItem listDtoToCreateDto(ModuleItemListDto dto);

    DashboardModuleItem listDtoToEntity(ModuleItemListDto dto);
}
