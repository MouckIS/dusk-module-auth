package com.dusk.module.auth.mapper;

import com.dusk.common.rpc.auth.dto.orga.OrganizationUnitDto;
import com.dusk.module.auth.dto.orga.*;
import com.dusk.module.auth.dto.station.StationsOfLoginUserDto;
import com.dusk.module.auth.dto.user.UserOrgaDto;
import com.dusk.module.auth.entity.OrganizationUnit;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper
public interface OrganizationMapper {
    OrganizationMapper INSTANCE = org.mapstruct.factory.Mappers.getMapper(OrganizationMapper.class);

    OrganizationUnitDto toDto(OrganizationUnit entity);

    OrganizationStationUnitDto toStationUnitDto(OrganizationUnit entity);

    void updateEntityFromDto(UpdateOrganizationUnitInput dto, @MappingTarget OrganizationUnit entity);

    OrganizationUnit toEntity(OrganizationUnitDto dto);

    StationsOfLoginUserDto toStationsOfLoginUserDto(OrganizationUnit entity);

    OrganizationUnit excelImportDtoToEntity(ImportOrganizationExcelDto dto);

    OrganizationUnit CreateOInputToEntity(CreateOrganizationUnitInput dto);

    ParentOrganizationOutput toParentOrganizationOutput(OrganizationUnit entity);

    UpdateOrganizationUnitInput toUpdateInput(OrganizationUnitDto dto);

    CreateOrganizationUnitInput toCreateInput(OrganizationUnitDto dto);

    UserOrgaDto toUserOrgaDto(OrganizationUnit entity);
}
